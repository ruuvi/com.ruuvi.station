package com.ruuvi.station.network.domain

import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.network.data.request.GetSensorDataRequest
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class NetworkDataRepository (
    val tagRepository: TagRepository,
    val networkInteractor: RuuviNetworkInteractor
) {
    val ioScope = CoroutineScope(Dispatchers.IO)

    val tagJobs = mutableMapOf<String, Job>()

    fun saveSensorData(sensorData: GetSensorDataResponseBody): Int {
        val dbData = TagSensorReading.getLatestForTag(sensorData.sensor, 1)
        val tag = tagRepository.getTagById(sensorData.sensor)

        if (tag != null && tag.isFavorite) {
            val list = mutableListOf<TagSensorReading>()
            sensorData.measurements.forEach {measurement ->
                val reading = BluetoothLibrary.decode(measurement.sensor, measurement.data, measurement.rssi)
                list.add(TagSensorReading(reading, tag, measurement.timestamp))
            }

            var since = Date(0)
            if (dbData.count() > 0) {
                since = dbData[0].createdAt
            }

            val saveList = list.filter { it.createdAt > since }
            if (saveList.size > 0) {
                val newestPoint = saveList.sortedByDescending { it.createdAt }.first()
                tag.updateData(newestPoint)
                tagRepository.updateTag(tag)
                TagSensorReading.saveList(saveList)
                return saveList.size
            }
        }
        return 0
    }

    fun getSensorDataForPeriod(tagId: String, period: Long, onResult: (String) -> Unit) {
        ioScope.launch {
            val last = tagRepository.getLatestForTag(tagId, 1)
            val tag = tagRepository.getTagById(tagId)

            if (tag != null && tag.isFavorite) {
                val cal = Calendar.getInstance()
                cal.time = Date()
                cal.add(Calendar.HOUR, -period.toInt())

                var since = cal.time

                if (last.isNotEmpty() && last[0].createdAt > since) since = last[0].createdAt

                var result = getSince(tagId, since, 100)
                val measurements = mutableListOf<SensorDataMeasurementResponse>()

                while (result != null && result?.data?.total ?: 0 > 0) {
                    val data = result.data?.measurements
                    if (data != null && data.size > 0) {
                        measurements.addAll(data)
                        var maxTimestamp = data.maxBy{ it.timestamp }?.timestamp
                        if (maxTimestamp != null) {
                            maxTimestamp++
                            since = Date(maxTimestamp * 1000)
                            result = getSince(tagId, since, 100)
                        } else {
                            result = null
                        }
                    }
                }

                if (measurements.size > 0) {
                    saveSensorData2(tag, measurements)
                    Timber.d("Bulk save for tag: $tagId. Data points count ${measurements.size}")
                }
            }
        }

        onResult("new job to get data scheduled")
    }

    fun saveSensorData2(tag: RuuviTagEntity, measurements: List<SensorDataMeasurementResponse>): Int {
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

    fun updateSensorsData(userInfo: UserInfoResponseBody?) {
        userInfo?.let { userInfo ->
            for (tagInfo in userInfo.sensors) {
                CoroutineScope(Dispatchers.Main).launch {
                    getSensorDataForPeriod(tagInfo.sensor, 72) {}
                }
            }
        }

    }
}