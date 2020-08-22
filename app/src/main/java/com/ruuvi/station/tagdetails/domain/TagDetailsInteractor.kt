package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagConverter

class TagDetailsInteractor(
    private val tagRepository: TagRepository,
    private val tagConverter: TagConverter
) {

    fun getTagById(tagId: String): RuuviTag? =
        tagRepository.getTagById(tagId)?.let { tagConverter.fromDatabase(it) }

    fun getTagReadings(tagId: String): List<TagSensorReading>? =
        tagRepository.getTagReadings(tagId)

    fun getTemperatureString(tag: RuuviTag): String =
        tagRepository.getTemperatureString(tag)

    fun getHumidityString(tag: RuuviTag): String =
        tagRepository.getHumidityString(tag)
}