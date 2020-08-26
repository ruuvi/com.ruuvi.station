package com.ruuvi.station.tagsettings.domain

import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import java.util.*

class HumidityCalibrationInteractor (private val tagRepository: TagRepository) {
    fun calibrate(tag: RuuviTagEntity) {
        val previousHumidityOffset = tag.humidityOffset ?: 0.0
        tag.humidity -= previousHumidityOffset
        tag.humidityOffset = 75.0 - (tag.humidity ?: 0.0)
        tag.humidityOffsetDate = Date()
        apply(tag)
        tagRepository.updateTag(tag)
    }

    fun clear(tag: RuuviTagEntity) {
        val previousHumidityOffset = tag.humidityOffset ?: 0.0
        tag.humidityOffset = 0.0
        tag.humidityOffsetDate = null
        tag.humidity -= previousHumidityOffset
        tagRepository.updateTag(tag)
    }

    fun apply(tag: RuuviTagEntity) {
        tag.humidity += (tag.humidityOffset ?: 0.0)
    }
}