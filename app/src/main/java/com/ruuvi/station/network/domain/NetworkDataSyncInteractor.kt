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
import com.ruuvi.station.network.data.NetworkSyncResult
import com.ruuvi.station.network.data.NetworkSyncResultType
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
import kotlinx.coroutines.flow.MutableStateFlow
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

    private val syncResult = MutableStateFlow<NetworkSyncResult> (NetworkSyncResult(NetworkSyncResultType.NONE))
    val syncResultFlow: StateFlow<NetworkSyncResult> = syncResult

    private val syncInProgress = MutableStateFlow<Boolean> (false)
    val syncInProgressFlow: StateFlow<Boolean> = syncInProgress

    fun startAutoRefresh() {
        Timber.d("startAutoRefresh")
        if (autoRefreshJob != null && autoRefreshJob?.isActive == true) {
            Timber.d("Already in auto refresh mode")
            return
        }

        autoRefreshJob = CoroutineScope(IO).launch {
            syncNetworkData(false)
            delay(10000)
            while (true) {
                val lastSync = preferencesRepository.getLastSyncDate()
                Timber.d("Cloud auto refresh another round. Last sync ${Date(lastSync)}")
                if (networkInteractor.signedIn &&
                    Date(lastSync).diffGreaterThan(60000)) {
                        Timber.d("Do actual sync")
                    syncNetworkData(false)
                }
                delay(10000)
            }
        }
    }

    fun stopAutoRefresh() {
        Timber.d("stopAutoRefresh")
        autoRefreshJob?.cancel()
    }

    fun syncNetworkData(ecoMode: Boolean): Job {
        if (syncJob.isActive == true) {
            Timber.d("Already in sync mode")
            return syncJob
        }

        setSyncInProgress(true)
        syncJob = CoroutineScope(IO).launch() {
            try {
                networkRequestExecutor.executeScheduledRequests()
                if (!networkRequestExecutor.anySettingsRequests()) {
                    networkApplicationSettings.updateSettingsFromNetwork()
                }

                val userInfo = networkInteractor.getUserInfo()

                if (userInfo?.data?.sensors == null) {
                    setSyncResult(NetworkSyncResult(NetworkSyncResultType.NOT_LOGGED))
                    return@launch
                }

                val benchUpdate1 = Date()
                updateSensors(userInfo.data, ecoMode)
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
                val message = exception.message
                message?.let {
                    Timber.e(exception, "NetworkSync Exception")
                    setSyncResult(NetworkSyncResult(NetworkSyncResultType.EXCEPTION, it))
                }
            } finally {
                setSyncInProgress(false)
            }
            setSyncInProgress(false)
        }
        return syncJob
    }

    private suspend fun syncForPeriod(userInfoData: UserInfoResponseBody, hours: Int) {
        if (!networkInteractor.signedIn) {
            setSyncResult(NetworkSyncResult(NetworkSyncResultType.NOT_LOGGED))
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

        setSyncResult(NetworkSyncResult(NetworkSyncResultType.SUCCESS))
        preferencesRepository.setLastSyncDate(Date().time)
    }

    suspend fun syncSensorDataForPeriod(sensorId: String, period: Int) {
        Timber.d("Synchronizing... $sensorId")

        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)

        if (sensorSettings != null) {
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.HOUR, -period)

            var since = cal.time
            if (sensorSettings.networkLastSync ?: Date(Long.MIN_VALUE) > since) since = sensorSettings.networkLastSync

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

    private suspend fun setSyncResult(status: NetworkSyncResult) = withContext(Dispatchers.Main) {
        Timber.d("SyncStatus = $status")
        syncResult.value = status
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

    private suspend fun updateSensors(userInfoData: UserInfoResponseBody, ecoMode: Boolean) {
        userInfoData.sensors.forEach { sensor ->
            Timber.d("updateTags: $sensor")
            val sensorSettings = sensorSettingsRepository.getSensorSettingsOrCreate(sensor.sensor)
            sensorSettings.updateFromNetwork(sensor, calibrationInteractor)

            if (!ecoMode && !sensor.picture.isNullOrEmpty()) {
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
            Timber.d("updating image $networkImageGuid")
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

    fun syncStatusShowed() {
        syncResult.value = NetworkSyncResult(NetworkSyncResultType.NONE)
    }

    fun stopSync() {
        Timber.d("stopSync")
        if (syncJob.isActive) {
            syncJob.cancel()
        }
    }
}