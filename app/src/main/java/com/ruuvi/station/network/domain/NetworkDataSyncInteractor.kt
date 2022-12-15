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
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.request.SensorDataMode
import com.ruuvi.station.network.data.request.SortMode
import com.ruuvi.station.network.data.response.GetSensorDataResponse
import com.ruuvi.station.network.data.response.SensorDataMeasurementResponse
import com.ruuvi.station.network.data.response.SensorDataResponse
import com.ruuvi.station.network.data.response.UserInfoResponseBody
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
    private val firebaseInteractor: FirebaseInteractor
) {
    @Volatile
    private var syncJob: Job = Job().also { it.complete() }

    @Volatile
    private var autoRefreshJob: Job? = null

    private val _syncEvents = MutableSharedFlow<NetworkSyncEvent> ()
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
            if (networkInteractor.signedIn) syncNetworkData()
            delay(10000)
            while (true) {
                val lastSync = preferencesRepository.getLastSyncDate()
                Timber.d("Cloud auto refresh another round. Last sync ${Date(lastSync)}")
                if (networkInteractor.signedIn &&
                    Date(lastSync).diffGreaterThan(60000)) {
                        Timber.d("Do actual sync")
                    syncNetworkData()
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

        setSyncInProgress(true)
        syncJob = CoroutineScope(IO).launch() {
            try {
                Timber.d("Sync job started")
                sendSyncEvent(NetworkSyncEvent.InProgress)
                Timber.d("executeScheduledRequests")
                networkRequestExecutor.executeScheduledRequests()

                if (!networkRequestExecutor.anySettingsRequests()) {
                    Timber.d("updateSettingsFromNetwork")
                    networkApplicationSettings.updateSettingsFromNetwork()
                }

                Timber.d("getUserInfo")
                val userInfo = networkInteractor.getUserInfo()
                Timber.d("userInfo = $userInfo")
                if (userInfo?.isError() == true || userInfo?.data == null) {
                    val event = when (userInfo?.code) {
                        NetworkResponseLocalizer.ER_UNAUTHORIZED -> NetworkSyncEvent.Unauthorised
                        else -> NetworkSyncEvent.Error(userInfo?.error ?: "Unknown error")
                    }
                    sendSyncEvent(event)
                    return@launch
                }

                val benchUpdate1 = Date()
                Timber.d("updateSensors")
                updateSensors(userInfo.data)
                firebaseInteractor.logSync(userInfo.data)
                val benchUpdate2 = Date()
                Timber.d("benchmark-updateTags-finish - ${benchUpdate2.time - benchUpdate1.time} ms")
                Timber.d("benchmark-syncForPeriod-start")
                val benchSync1 = Date()
                syncForPeriod(userInfo.data, GlobalSettings.historyLengthHours)
                val benchSync2 = Date()
                Timber.d("benchmark-syncForPeriod-finish - ${benchSync2.time - benchSync1.time} ms")
                networkAlertsSyncInteractor.updateAlertsFromNetwork()
            } catch (exception: Exception) {
                exception.message?.let { message ->
                    Timber.e(exception, "NetworkSync Exception")
                    sendSyncEvent(NetworkSyncEvent.Error(message))

                }
            } finally {
                sendSyncEvent(NetworkSyncEvent.Idle)
                setSyncInProgress(false)
            }
        }
        return syncJob
    }

    private suspend fun syncForPeriod(userInfoData: UserInfoResponseBody, hours: Int) {
        if (!networkInteractor.signedIn) {
            return
        }

        withContext(IO) {
            val tagJobs = mutableListOf<Job>()
            for (tagInfo in userInfoData.sensors) {

                val job = launch {
                    Timber.d("benchmark-syncSensorDataForPeriod-${tagInfo.sensor}-start")
                    val benchUpdate1 = Date()
                    syncSensorDataForPeriod(tagInfo.sensor, hours)
                    val benchUpdate2 = Date()
                    Timber.d("benchmark-syncSensorDataForPeriod-${tagInfo.sensor}-finish - ${benchUpdate2.time - benchUpdate1.time} ms")
                }
                tagJobs.add(job)
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
            val lastSync = sensorSettings.networkLastSync ?: Date(Long.MIN_VALUE)
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
                saveSensorData( sensorSettings, measurements)
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

    private fun setSyncInProgress(status: Boolean) {//} = withContext(Dispatchers.Main) {
        Timber.d("SyncInProgress = $status")
        syncInProgress.value = status
    }

    private fun saveSensorData(sensorSettings: SensorSettings, measurements: List<SensorDataMeasurementResponse>): Int {
        val sensorId = sensorSettings.id
        val list = measurements.mapNotNull { measurement ->
            try {
                if (measurement.data.isNotEmpty()) {
                    val reading = TagSensorReading(
                        BluetoothLibrary.decode(sensorId, measurement.data, measurement.rssi),
                        Date(measurement.timestamp * 1000)
                    )
                    sensorSettings.calibrateSensor(reading)
                    reading
                } else {
                    null
                }
            } catch (e: Exception) {
                Timber.e(e, "NetworkData: $sensorId measurement = $measurement")
                null
            }
        }
        val newestPoint = list.maxByOrNull { it.createdAt }

        if (list.isNotEmpty() && newestPoint != null) {
            tagRepository.activateSensor(newestPoint)
            sensorHistoryRepository.bulkInsert(sensorId, list)
            sensorSettingsRepository.updateNetworkLastSync(sensorId, newestPoint.createdAt)
            return list.size
        }
        return 0
    }

    private suspend fun updateSensors(userInfoData: UserInfoResponseBody) {
        userInfoData.sensors.forEach { sensor ->
            Timber.d("updateTags: $sensor")
            val sensorSettings = sensorSettingsRepository.getSensorSettingsOrCreate(sensor.sensor)
            sensorSettings.updateFromNetwork(sensor, calibrationInteractor)

            val tagEntry = tagRepository.getTagById(sensor.sensor)
            if (tagEntry?.favorite == false) {
                tagEntry.favorite = true
                tagEntry.update()
            }

            if (!sensor.picture.isNullOrEmpty()) {
                setSensorImage(sensor, sensorSettings)
            }
        }

        val sensors = sensorSettingsRepository.getSensorSettings()
        for (sensor in sensors) {
            if (sensor.networkSensor && userInfoData.sensors.none { it.sensor == sensor.id }) {
                tagRepository.deleteSensorAndRelatives(sensor.id)
            }
        }
    }

    private suspend fun setSensorImage(sensor: SensorDataResponse, sensorSettings: SensorSettings) {
        val networkImageGuid = File(URI(sensor.picture).path).nameWithoutExtension

        if (networkImageGuid != sensorSettings.networkBackground) {
            Timber.d("updating image $networkImageGuid ${sensorSettings}")
            try {
                val imageFile = imageInteractor.downloadImage(
                    "cloud_${sensor.sensor}",
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