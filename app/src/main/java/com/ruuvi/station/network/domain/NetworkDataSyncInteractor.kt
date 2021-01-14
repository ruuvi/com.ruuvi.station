package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.network.data.NetworkSyncResult
import com.ruuvi.station.network.data.NetworkSyncResultType
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.response.GetSensorDataResponse
import com.ruuvi.station.network.data.response.SensorDataMeasurementResponse
import com.ruuvi.station.network.data.response.UserInfoResponseBody
import com.ruuvi.station.util.extensions.diffGreaterThan
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.util.*

@ExperimentalCoroutinesApi
class NetworkDataSyncInteractor (
    private val preferencesRepository: PreferencesRepository,
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor
) {
    @Volatile
    private var syncJob: Job? = null

    private val syncResult = MutableStateFlow<NetworkSyncResult> (NetworkSyncResult(NetworkSyncResultType.NONE))
    val syncResultFlow: StateFlow<NetworkSyncResult> = syncResult

    private val syncInProgress = MutableStateFlow<Boolean> (false)
    val syncInProgressFlow: StateFlow<Boolean> = syncInProgress

    fun syncNetworkData() {
        if (syncJob != null && syncJob?.isActive == true) {
            Timber.d("Already in sync mode")
            return
        }

        syncJob = CoroutineScope(IO).launch() {
            try {
                setSyncInProgress(true)

                val userInfo = networkInteractor.getUserInfo()

                if (userInfo?.data?.sensors == null) {
                    setSyncResult(NetworkSyncResult(NetworkSyncResultType.NOT_LOGGED))
                    return@launch
                }

                val benchUpdate1 = Date()
                updateTags(userInfo.data)
                val benchUpdate2 = Date()
                Timber.d("benchmark-updateTags-finish - ${benchUpdate2.time - benchUpdate1.time} ms")
                Timber.d("benchmark-syncForPeriod-start")
                val benchSync1 = Date()
                syncForPeriod(userInfo.data, 72)
                val benchSync2 = Date()
                Timber.d("benchmark-syncForPeriod-finish - ${benchSync2.time - benchSync1.time} ms")
            } catch (exception: Exception) {
                val message = exception.message
                message?.let {
                    setSyncResult(NetworkSyncResult(NetworkSyncResultType.EXCEPTION, it))
                }
            } finally {
                setSyncInProgress(false)
            }
            setSyncInProgress(false)
        }
    }

    private suspend fun syncForPeriod(userInfoData: UserInfoResponseBody, hours: Int) {
        if (networkInteractor.signedIn == false) {
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

    suspend fun syncSensorDataForPeriod(tagId: String, period: Int) {
        Timber.d("Synchronizing... $tagId")

        val tag = tagRepository.getTagById(tagId)

        if (tag != null && tag.isFavorite) {
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.HOUR, -period)

            var since = cal.time
            if (tag.networkLastSync ?: Date(Long.MIN_VALUE) > since) since = tag.networkLastSync
            val originalSince = since

            // if we have data for recent minute - skipping update
            if (!since.diffGreaterThan(60*1000)) return

            val benchRequest1 = Date()

            var result = getSince(tagId, since, 5000)
            val measurements = mutableListOf<SensorDataMeasurementResponse>()

            var count = 1
            while (result != null && result.data?.total ?: 0 > 0) {
                val data = result.data?.measurements
                result = null
                if (data != null && data.size > 0) {
                    measurements.addAll(data)
                    var maxTimestamp = data.maxBy { it.timestamp }?.timestamp
                    if (maxTimestamp != null) {
                        maxTimestamp++
                        since = Date(maxTimestamp * 1000)
                        if (since.diffGreaterThan(60*1000)) {
                            result = getSince(tagId, since, 5000)
                            count++
                        }
                    }
                }
            }
            val benchRequest2 = Date()
            Timber.d("benchmark-getSensorData($count)-finish ${tagId} - ${benchRequest2.time - benchRequest1.time} ms")


            if (measurements.size > 0) {
                val benchUpdate1 = Date()
                val existData = tagRepository.getTagReadingsDate(tagId, originalSince)?.map { it.createdAt }
                saveSensorData(tag, measurements, existData ?: listOf())
                val benchUpdate2 = Date()
                Timber.d("benchmark-saveSensorData-finish ${tagId} Data points count ${measurements.size} - ${benchUpdate2.time - benchUpdate1.time} ms")
            }
        }
    }

    private suspend fun setSyncResult(status: NetworkSyncResult) = withContext(Dispatchers.Main) {
        Timber.d("SyncStatus = $status")
        syncResult.value = status
    }

    private suspend fun setSyncInProgress(status: Boolean) = withContext(Dispatchers.Main) {
        Timber.d("SyncInProgress = $status")
        syncInProgress.value = status
    }

    private fun saveSensorData(tag: RuuviTagEntity, measurements: List<SensorDataMeasurementResponse>, existsData: List<Date>): Int {
        val list = mutableListOf<TagSensorReading>()
        measurements.forEach { measurement ->
            val createdAt = Date(measurement.timestamp * 1000)
            if (!existsData.contains(createdAt)) {
                val reading = BluetoothLibrary.decode(tag.id!!, measurement.data, measurement.rssi)
                list.add(TagSensorReading(reading, tag, createdAt))
            }
        }

        if (list.size > 0) {
            val newestPoint = list.sortedByDescending { it.createdAt }.first()
            tag.updateData(newestPoint)
            tagRepository.updateTag(tag)
            TagSensorReading.saveList(list)
            return list.size
        }
        return 0
    }

    private fun updateTags(userInfoData: UserInfoResponseBody) {
        userInfoData.sensors.forEach { sensor ->
            var tagDb = tagRepository.getTagById(sensor.sensor)
            if (tagDb == null) {
                tagDb = RuuviTagEntity()
                tagDb.id = sensor.sensor
                tagDb.name = if (sensor.name.isEmpty()) sensor.sensor else sensor.name
                tagDb.favorite = true
                tagDb.defaultBackground = (Math.random() * 9.0).toInt()
                tagDb.updateAt = Date()
                tagDb.insert()
            } else {
                tagDb.favorite = true
                tagDb.updateAt = Date()
                if (sensor.name.isNotEmpty()) tagDb.name = sensor.name
                tagDb.update()
            }
        }
    }

    suspend fun getSince(tagId: String, since: Date, limit: Int): GetSensorDataResponse? {
        Timber.d("benchmark-getSince-$tagId since $since")
        val request = GetSensorDataRequest(
            tagId,
            since = since,
            sort = "asc",
            limit = limit
        )
        return networkInteractor.getSensorData(request)
    }

    fun syncStatusShowed() {
        syncResult.value = NetworkSyncResult(NetworkSyncResultType.NONE)
    }
}