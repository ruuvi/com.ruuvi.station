package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading

class TagDetailsInteractor(
    private val tagRepository: TagRepository
) {

    fun getTagById(tagId: String): RuuviTagEntity? = tagRepository.getTagById(tagId)

    fun getTagReadings(tagId: String): List<TagSensorReading>? =
        tagRepository.getTagReadings(tagId)

    fun getTemperatureString(tag: RuuviTagEntity): String =
        tagRepository.getTemperatureString(tag)

    fun getHumidityString(tag: RuuviTagEntity): String =
        tagRepository.getHumidityString(tag)
}