package com.ruuvi.station.network.domain

import android.net.Uri
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.firebase.domain.PushRegisterInteractor
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.image.ImageSource
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.request.SensorDataMode
import com.ruuvi.station.network.data.request.SensorDenseRequest
import com.ruuvi.station.network.data.request.SortMode
import com.ruuvi.station.network.data.response.*
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.util.extensions.diffGreaterThan
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.io.File
import java.net.URI
import java.util.*

class NetworkDataSyncInteractor (
    private val preferencesRepository: PreferencesRepository,
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val imageInteractor: ImageInteractor,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val networkRequestExecutor: NetworkRequestExecutor,
    private val networkApplicationSettings: NetworkApplicationSettings,
    private val networkAlertsSyncInteractor: NetworkAlertsSyncInteractor,
    private val calibrationInteractor: CalibrationInteractor,
    private val firebaseInteractor: FirebaseInteractor,
    private val tagSettingsInteractor: TagSettingsInteractor,
    private val pushRegisterInteractor: PushRegisterInteractor,
    private val networkShareListInteractor: NetworkShareListInteractor,
    private val subscriptionInfoSyncInteractor: SubscriptionInfoSyncInteractor
) {
    @Volatile
    private var syncJob: Job = Job().also { it.complete() }

    @Volatile
    private var autoRefreshJob: Job? = null

    private val _syncEvents = MutableSharedFlow<NetworkSyncEvent> (replay = 1)
    val syncEvents: SharedFlow<NetworkSyncEvent> = _syncEvents

    private val syncInProgress = MutableStateFlow<Boolean> (false)
    val syncInProgressFlow: StateFlow<Boolean> = syncInProgress

    fun startAutoRefresh() {
        Timber.d("startAutoRefresh isSignedIn = ${networkInteractor.signedIn}")
        if (autoRefreshJob != null && autoRefreshJob?.isActive == true) {
            Timber.d("Already in auto refresh mode")
            return
        }

        autoRefreshJob = CoroutineScope(IO).launch {
            if (networkInteractor.signedIn) {
                pushRegisterInteractor.checkAndRegisterDeviceToken()
                syncNetworkData()
            }
            delay(10000)
            while (true) {
                val lastSync = preferencesRepository.getLastSyncDate()
                Timber.d("Cloud auto refresh another round. Last sync ${Date(lastSync)}")
                if (Date(lastSync).diffGreaterThan(60000)) {
                    pushRegisterInteractor.checkAndRegisterDeviceToken()
                    if (networkInteractor.signedIn) {
                        Timber.d("Do actual sync")
                        syncNetworkData()
                    }
                }
                delay(10000)
            }
        }
    }

    fun stopAutoRefresh() {
        Timber.d("stopAutoRefresh")
        autoRefreshJob?.cancel()
    }

    fun syncNetworkData(): Job {
        if (syncJob.isActive == true) {
            Timber.d("Already in sync mode")
            return syncJob
        }

        val userEmail = networkInteractor.getEmail()
        if (userEmail.isNullOrEmpty() || !networkInteractor.signedIn) {
            Timber.d("Not signed in")
            return syncJob
        }

        val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Timber.e(throwable, "NetworkSync Exception")
        }

        setSyncInProgress(true)
        syncJob = CoroutineScope(IO + coroutineExceptionHandler).launch() {
            try {
                Timber.d("Sync job started")
                sendSyncEvent(NetworkSyncEvent.InProgress)
                Timber.d("executeScheduledRequests")
                networkRequestExecutor.executeScheduledRequests()

                subscriptionInfoSyncInteractor.syncSubscriptionInfo()

                if (!networkRequestExecutor.anySettingsRequests()) {
                    Timber.d("updateSettingsFromNetwork")
                    networkApplicationSettings.updateSettingsFromNetwork()
                }

                Timber.d("get getSensorDenseLastData")
                val sensorsRequest = SensorDenseRequest(
                    sensor = null,
                    measurements = true,
                    alerts = true,
                    sharedToOthers = true,
                    sharedToMe = true
                )

                val sensorsInfo = networkInteractor.getSensorDenseLastData(sensorsRequest)
                Timber.d("sensorsInfo = $sensorsInfo")
                if (sensorsInfo?.isError() == true || sensorsInfo?.data == null) {
                    val event = when (sensorsInfo?.code) {
                        NetworkResponseLocalizer.ER_UNAUTHORIZED -> NetworkSyncEvent.Unauthorised
                        else -> NetworkSyncEvent.Error(sensorsInfo?.error ?: "Unknown error")
                    }
                    sendSyncEvent(event)
                    return@launch
                }

                val benchUpdate1 = Date()
                Timber.d("updateSensors")
                updateSensors(sensorsInfo.data)
                sendSyncEvent(NetworkSyncEvent.SensorsSynced)
                updateBackgrounds(sensorsInfo.data)
                firebaseInteractor.logSync(userEmail, sensorsInfo.data)
                val benchUpdate2 = Date()
                Timber.d("benchmark-updateTags-finish - ${benchUpdate2.time - benchUpdate1.time} ms")
                Timber.d("benchmark-syncForPeriod-start")
                val benchSync1 = Date()
                syncForPeriod(sensorsInfo.data, GlobalSettings.historyLengthHours)
                val benchSync2 = Date()
                Timber.d("benchmark-syncForPeriod-finish - ${benchSync2.time - benchSync1.time} ms")
                networkAlertsSyncInteractor.updateAlertsFromNetwork(sensorsInfo)
                networkShareListInteractor.updateSharingInfo(sensorsInfo)
                networkRequestExecutor.executeScheduledRequests()
            }
            catch (exception: Exception) {
                exception.message?.let { message ->
                    sendSyncEvent(NetworkSyncEvent.Error(message))
                }
            } finally {
                sendSyncEvent(NetworkSyncEvent.Idle)
                setSyncInProgress(false)
            }
        }
        return syncJob
    }

    private suspend fun syncForPeriod(userInfoData: SensorsDenseResponseBody, hours: Int) {
        if (!networkInteractor.signedIn) {
            return
        }

        withContext(IO) {
            val tagJobs = mutableListOf<Job>()
            for (tagInfo in userInfoData.sensors) {

                if (tagInfo.subscription.maxHistoryDays > 0) {
                    val job = launch {
                        Timber.d("benchmark-syncSensorDataForPeriod-${tagInfo.sensor}-start")
                        val benchUpdate1 = Date()
                        syncSensorDataForPeriod(tagInfo.sensor, hours)
                        val benchUpdate2 = Date()
                        Timber.d("benchmark-syncSensorDataForPeriod-${tagInfo.sensor}-finish - ${benchUpdate2.time - benchUpdate1.time} ms")
                    }
                    tagJobs.add(job)
                }
            }
            for (job in tagJobs) {
                job.join()
            }
        }

        sendSyncEvent(NetworkSyncEvent.Success)
        preferencesRepository.setLastSyncDate(Date().time)
    }

    suspend fun syncSensorDataForPeriod(sensorId: String, period: Int) {
        Timber.d("Synchronizing... $sensorId")

        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)

        if (sensorSettings != null) {
            val calendar = Calendar.getInstance()
            calendar.time = Date()
            calendar.add(Calendar.HOUR, -period)

            var since = calendar.time
            val lastSync = sensorSettings.networkHistoryLastSync ?: Date(Long.MIN_VALUE)
            if (lastSync > since) {
                calendar.time = lastSync
                calendar.add(Calendar.SECOND, 1)
                since = calendar.time
            }

            // if we have data for recent minute - skipping update
            if (!since.diffGreaterThan(60*1000)) return

            val benchRequest1 = Date()

            var result = getSince(sensorId, since, 5000)
            val measurements = mutableListOf<SensorDataMeasurementResponse>()

            var count = 1
            while (result != null && result.data?.total ?: 0 > 0) {
                val data = result.data?.measurements
                result = null
                if (data != null && data.size > 0) {
                    measurements.addAll(data)
                    var maxTimestamp = data.maxByOrNull { it.timestamp }?.timestamp
                    if (maxTimestamp != null) {
                        maxTimestamp++
                        since = Date(maxTimestamp * 1000)
                        if (since.diffGreaterThan(60*1000)) {
                            result = getSince(sensorId, since, 5000)
                            count++
                        }
                    }
                }
            }
            val benchRequest2 = Date()
            Timber.d("benchmark-getSensorData($count)-finish ${sensorId} - ${benchRequest2.time - benchRequest1.time} ms")


            if (measurements.size > 0) {
                val benchUpdate1 = Date()
                saveSensorHistory( sensorSettings, measurements)
                val benchUpdate2 = Date()
                Timber.d("benchmark-saveSensorData-finish ${sensorId} Data points count ${measurements.size} - ${benchUpdate2.time - benchUpdate1.time} ms")
            }
        }
    }

    private suspend fun sendSyncEvent(event: NetworkSyncEvent) {
        Timber.d("SyncEvent = $event")
        CoroutineScope(IO).launch() {
            _syncEvents.emit(event)
        }
    }

    private fun setSyncInProgress(status: Boolean) {
        Timber.d("SyncInProgress = $status")
        syncInProgress.value = status
    }

    private fun saveSensorHistory(sensorSettings: SensorSettings, measurements: List<SensorDataMeasurementResponse>): Int {
        val sensorId = sensorSettings.id
        val list = measurements.mapNotNull { measurement ->
            preparePoint(sensorSettings, measurement)
        }
        val newestPoint = list.maxByOrNull { it.createdAt }

        if (list.isNotEmpty() && newestPoint != null) {
            sensorHistoryRepository.bulkInsert(sensorId, list)
            sensorSettingsRepository.updateNetworkHistoryLastSync(sensorId, newestPoint.createdAt)
            return list.size
        }
        return 0
    }

    private fun preparePoint(sensorSettings: SensorSettings, measurement: SensorDataMeasurementResponse): TagSensorReading? {
        return try {
            if (measurement.data.isNotEmpty()) {
                val reading = TagSensorReading(
                    BluetoothLibrary.decode(sensorSettings.id, measurement.data, measurement.rssi),
                    Date(measurement.timestamp * 1000)
                )
                reading
            } else {
                null
            }
        }
        catch (e: Exception) {
            Timber.e(e, "NetworkData: ${sensorSettings.id} measurement = $measurement")
            null
        }
    }

    private suspend fun updateSensors(userInfoData: SensorsDenseResponseBody) {
        userInfoData.sensors.forEach { sensor ->
            Timber.d("updateTags: $sensor")
            val sensorSettings = sensorSettingsRepository.getSensorSettingsOrCreate(sensor.sensor)
            sensorSettings.updateFromNetwork(sensor)

            val tagEntry = tagRepository.getTagById(sensor.sensor)
            if (tagEntry?.favorite == false) {
                tagEntry.favorite = true
                tagEntry.update()
            }

            val latestData = sensor.measurements.maxByOrNull { it.timestamp }
            if (latestData != null) {
                updateLatestMeasurement(
                    sensorSettings = sensorSettings,
                    measurement = latestData,
                    saveToHistory = sensor.subscription.maxHistoryDays == 0
                )
            }
        }

        val sensors = sensorSettingsRepository.getSensorSettings()
        for (sensor in sensors) {
            if (sensor.networkSensor && userInfoData.sensors.none { it.sensor == sensor.id }) {
                tagRepository.deleteSensorAndRelatives(sensor.id)
            }
        }
    }

    private suspend fun updateBackgrounds(userInfoData: SensorsDenseResponseBody) {
        userInfoData.sensors.forEach { sensor ->
            val sensorSettings = sensorSettingsRepository.getSensorSettingsOrCreate(sensor.sensor)

            if (sensor.picture.isNullOrEmpty()) {
                tagSettingsInteractor.setDefaultBackgroundImageByResource(
                    sensorId = sensor.sensor,
                    defaultBackground = imageInteractor.getDefaultBackgroundById(sensorSettings.defaultBackground),
                    uploadNow = true
                )
            } else {
                setSensorImage(sensor, sensorSettings)
            }
        }
    }

    private fun updateLatestMeasurement(
        sensorSettings: SensorSettings,
        measurement: SensorDataMeasurementResponse,
        saveToHistory: Boolean
    ) {
        val lastPoint = preparePoint(sensorSettings, measurement)
        if (lastPoint != null) {
            val freshPoint = (sensorSettings.networkLastSync?.time ?: Long.MIN_VALUE) < lastPoint.createdAt.time
            tagRepository.activateSensor(lastPoint)
            sensorSettingsRepository.updateNetworkLastSync(sensorSettings.id, lastPoint.createdAt)
            if (saveToHistory && freshPoint) {
                sensorHistoryRepository.insertPoint(lastPoint)
            }
        }
    }

    private suspend fun setSensorImage(sensor: SensorsDenseInfo, sensorSettings: SensorSettings) {
        if (networkRequestExecutor.gotAnyImagesInSync(sensor.sensor)) return

        val networkImageGuid = File(URI(sensor.picture).path).nameWithoutExtension

        if (networkImageGuid != sensorSettings.networkBackground) {
            Timber.d("updating image $networkImageGuid ${sensorSettings}")
            try {
                val imageFile = imageInteractor.downloadImage(
                    imageInteractor.getFilename(sensor.sensor, ImageSource.CLOUD),
                    sensor.picture
                )
                sensorSettingsRepository.updateSensorBackground(
                    sensor.sensor,
                    Uri.fromFile(imageFile).toString(),
                    null,
                    networkImageGuid
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load image: ${sensor.picture}")
            }
        }
    }

    suspend fun getSince(tagId: String, since: Date, limit: Int): GetSensorDataResponse? {
        Timber.d("benchmark-getSince-$tagId since $since")
        val request = GetSensorDataRequest(
            sensor = tagId,
            since = since,
            sort = SortMode.ASCENDING,
            limit = limit,
            mode = SensorDataMode.MIXED
        )
        return networkInteractor.getSensorData(request)
    }

    fun stopSync() {
        Timber.d("stopSync")
        syncInProgress.value = false
        if (syncJob.isActive) {
            syncJob.cancel()
        }
    }
}