package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagConverter
import com.ruuvi.station.units.domain.UnitsConverter
import java.util.*

class TagDetailsInteractor(
    private val tagRepository: TagRepository,
    private val tagConverter: TagConverter,
    private val unitsConverter: UnitsConverter

) {

    fun getTagById(tagId: String): RuuviTag? =
        tagRepository.getTagById(tagId)?.let { tagConverter.fromDatabase(it) }

    fun updateLastSync(tagId: String, date: Date?): RuuviTag? =
            tagRepository.getTagById(tagId)?.let {
                it.lastSync = date
                it.update()
                return tagConverter.fromDatabase(it)
            }

    fun getTagReadings(tagId: String): List<TagSensorReading>? =
        tagRepository.getTagReadings(tagId)

    fun getTemperatureString(tag: RuuviTag): String =
        unitsConverter.getTemperatureString(tag.temperature)

    fun getTemperatureStringWithoutUnit(tag: RuuviTag): String =
        unitsConverter.getTemperatureStringWithoutUnit(tag.temperature)

    fun getTemperatureUnitString(): String =
        unitsConverter.getTemperatureUnitString()

    fun getHumidityString(tag: RuuviTag): String =
        unitsConverter.getHumidityString(tag.humidity, tag.temperature)

    fun getPressureString(tag: RuuviTag): String =
        unitsConverter.getPressureString(tag.pressure)

    fun getSignalString(tag: RuuviTag): String =
        unitsConverter.getSignalString(tag.rssi)
}