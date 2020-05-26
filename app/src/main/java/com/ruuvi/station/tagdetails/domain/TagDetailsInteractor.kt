package com.ruuvi.station.tagdetails.domain

import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading

class TagDetailsInteractor {
    fun getAllTags(): List<RuuviTagEntity> = RuuviTagRepository.getAll(true)

    fun getTag(tagId: String): RuuviTagEntity? = RuuviTagRepository.get(tagId)

    fun getTagReadings(tagId: String): List<TagSensorReading>? = TagSensorReading.getForTag(tagId)
}