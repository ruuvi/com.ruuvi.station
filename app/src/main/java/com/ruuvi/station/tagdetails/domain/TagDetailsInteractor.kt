package com.ruuvi.station.tagdetails.domain

import android.content.Context
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading

class TagDetailsInteractor(val preferences: Preferences) {

    fun getAllTags(): List<RuuviTagEntity> = RuuviTagRepository.getAll(true)

    fun getTag(tagId: String): RuuviTagEntity? = RuuviTagRepository.get(tagId)

    fun getTagReadings(tagId: String): List<TagSensorReading>? = TagSensorReading.getForTagPruned(
            tagId,
            preferences.graphPointInterval,
            preferences.graphViewPeriod
    )

    fun getTemperatureString(context: Context, tag: RuuviTagEntity): String =
        RuuviTagRepository.getTemperatureString(context, tag)

    fun getHumidityString(context: Context, tag: RuuviTagEntity): String =
        RuuviTagRepository.getHumidityString(context, tag)
}