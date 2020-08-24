package com.ruuvi.station.tagsettings.domain

import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import java.util.*

class HumidityCalibrationInteractor()
{
    fun calibrate(tag: RuuviTagEntity) {
        val previousHumidityOffset = tag.humidityOffset ?: 0.0
        tag.humidity -= previousHumidityOffset
        tag.humidityOffset = 75.0 - (tag.humidity ?: 0.0)
        tag.humidityOffsetDate = Date()
        apply(tag)
        RuuviTagRepository.update(tag)
    }

    fun clear(tag: RuuviTagEntity) {
        val previousHumidityOffset = tag.humidityOffset ?: 0.0
        tag.humidityOffset = 0.0
        tag.humidityOffsetDate = null
        tag.humidity -= previousHumidityOffset
        RuuviTagRepository.update(tag)
    }

    fun apply(tag: RuuviTagEntity) {
        tag.humidity += (tag.humidityOffset ?: 0.0)
    }

}