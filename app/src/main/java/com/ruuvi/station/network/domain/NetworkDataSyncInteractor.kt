package com.ruuvi.station.network.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.lang.Exception
import java.util.*

@ExperimentalCoroutinesApi
class NetworkDataSyncInteractor (
    private val preferencesRepository: PreferencesRepository,
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor
) {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    @Volatile
    private var syncJob: Job? = null

    private val syncStatus = MutableStateFlow<String> ("")
    val syncStatusFlow: StateFlow<String> = syncStatus

    private val syncInProgress = MutableStateFlow<Boolean> (false)
    val syncInProgressFlow: StateFlow<Boolean> = syncInProgress

    fun syncNetworkData() {
        if (syncJob != null && syncJob?.isActive == true) {
            Timber.d("Already in sync mode")
            return
        }

        syncJob = ioScope.launch {
            try {
                setSyncInProgress(true)
                syncForPeriod(72)
            } catch (exception: Exception) {
                val message = exception.message
                message?.let {
                    setSyncFailedStatus(it)
                }
            } finally {
                setSyncInProgress(false)
            }
            setSyncInProgress(false)
        }
    }

    private suspend fun syncForPeriod(hours: Int) {
        if (networkInteractor.signedIn == false) {
            setSyncFailedStatus("can't sync if user not signed in")
            return
        }

        val userInfo = networkInteractor.getUserInfo()

        if (userInfo?.data?.sensors == null) {
            setSyncFailedStatus("can't get user info")
            return
        }

        val tagJobs = mutableListOf<Job>()
        for (tagInfo in userInfo.data.sensors) {
            val job = ioScope.launch {
                syncSensorDataForPeriod(tagInfo.sensor, 72)
            }
            tagJobs.add(job)
        }

        for (job in tagJobs) {
            job.join()
        }

        setSyncStatus("Synchronized successfully!")
        preferencesRepository.setLastSyncDate(Date().time)
    }

    suspend fun syncSensorDataForPeriod(tagId: String, period: Long) {
        Timber.d("Synchronizing... $tagId")

        val last = tagRepository.getLatestForTag(tagId, 1)
        val tag = tagRepository.getTagById(tagId)

        if (tag != null && tag.isFavorite) {
            val cal = Calendar.getInstance()
            cal.time = Date()
            cal.add(Calendar.HOUR, -period.toInt())

            var since = cal.time

            if (last.isNotEmpty() && last[0].createdAt > since) since = last[0].createdAt

            var result = getSince(tagId, since, 1000)
            val measurements = mutableListOf<SensorDataMeasurementResponse>()

            while (result != null && result?.data?.total ?: 0 > 0) {
                val data = result.data?.measurements
                if (data != null && data.size > 0) {
                    measurements.addAll(data)
                    var maxTimestamp = data.maxBy { it.timestamp }?.timestamp
                    if (maxTimestamp != null) {
                        maxTimestamp++
                        since = Date(maxTimestamp * 1000)
                        result = getSince(tagId, since, 1000)
                    } else {
                        result = null
                    }
                }
            }

            if (measurements.size > 0) {
                Timber.d("Bulk save for tag: $tagId. Data points count ${measurements.size}")
                saveSensorData(tag, measurements)
            }
        }
    }

    private suspend fun setSyncFailedStatus(status: String) = withContext(Dispatchers.Main) {
        Timber.d("SyncStatus = $status")
        syncStatus.value = "Synchronization failed: $status"
    }

    private suspend fun setSyncStatus(status: String) = withContext(Dispatchers.Main) {
        Timber.d("SyncStatus = $status")
        syncStatus.value = status
    }

    private suspend fun setSyncInProgress(status: Boolean) = withContext(Dispatchers.Main) {
        Timber.d("SyncInProgress = $status")
        syncInProgress.value = status
    }

    fun saveSensorData(tag: RuuviTagEntity, measurements: List<SensorDataMeasurementResponse>): Int {
        if (tag != null && tag.isFavorite) {
            val list = mutableListOf<TagSensorReading>()
            measurements.forEach {measurement ->
                if (measurement.data != null && measurement.rssi != null && measurement.timestamp != null) {
                    val reading = BluetoothLibrary.decode(tag.id!!, measurement.data, measurement.rssi)
                    list.add(TagSensorReading(reading, tag, measurement.timestamp))
                }
            }

            if (list.size > 0) {
                val newestPoint = list.sortedByDescending { it.createdAt }.first()
                tag.updateData(newestPoint)
                tagRepository.updateTag(tag)
                TagSensorReading.saveList(list)
                return list.size
            }
        }
        return 0
    }

    suspend fun getSince(tagId: String, since: Date, limit: Int): GetSensorDataResponse? {
        val request = GetSensorDataRequest(
            tagId,
            since = since,
            sort = "asc",
            limit = limit
        )
        return networkInteractor.getSensorData(request)
    }

    private fun updateSensorsData(userInfo: UserInfoResponseBody?) {
        userInfo?.let { userInfo ->
            for (tagInfo in userInfo.sensors) {
                CoroutineScope(Dispatchers.Main).launch {
                    syncSensorDataForPeriod(tagInfo.sensor, 72)
                }
            }
        }
    }

    fun syncStatusShowed() {
        syncStatus.value = ""
    }
}