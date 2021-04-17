package com.ruuvi.station.util.test

import com.ruuvi.station.bluetooth.FoundRuuviTag
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import timber.log.Timber
import java.util.*

class StressTestGenerator {
    companion object {
        val sensorHistoryRepository = SensorHistoryRepository()
        fun generateData(tagsCount: Int, sensorReadingsPerTag: Int, repository: TagRepository) {
            Timber.d("Generate $tagsCount fake tags and add $sensorReadingsPerTag readings for each")
            var sensorReadingsPerTag = sensorReadingsPerTag
            if (sensorReadingsPerTag > 20000)
                sensorReadingsPerTag = 20000

            for (tagNum in 1..tagsCount) {
                generateTag(tagNum, sensorReadingsPerTag, repository)
            }
            Timber.d("Generating done")
        }

        private fun generateTag(tagNum: Int, sensorReadingsPerTag: Int, repository: TagRepository) {
            val tagId = "FAKE:GEN:${tagNum.toString().padStart(4, '0')}"
            Timber.d("generating for $tagId")
            val newTag = FoundRuuviTag().apply {
                id = tagId
                accelX = -0.013
                accelY = 0.013
                accelZ = 1.046
                dataFormat = 3
                humidity = 43.0
                pressure = 101371.0
                rssi = -73
                temperature = 25.71
                voltage = 2.995
            }
            val tag = RuuviTagEntity(newTag)
            val dbtag = tag.id?.let { repository.getTagById(it) }
            if (dbtag == null) {
                tag.favorite = true
                tag.updateAt = Date()
                repository.saveTag(tag)
            }
            val calendar = Calendar.getInstance()
            val latest = sensorHistoryRepository.getLatestForSensor(tagId, 1)
            if (latest != null && latest.size > 0) {
                calendar.time = latest[0].createdAt
            } else {
                calendar.add(Calendar.HOUR, -24)
            }

            val intervalSeconds = 86400 / sensorReadingsPerTag
            val reading = TagSensorReading(tag)
            while (calendar.time < Date()) {
                reading.id = 0
                reading.createdAt = calendar.time
                reading.save()
                calendar.add(Calendar.SECOND, intervalSeconds)
            }
        }
    }
}