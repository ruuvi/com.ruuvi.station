package com.ruuvi.station.database

import android.content.Context
import androidx.annotation.NonNull
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.Alarm_Table
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.RuuviTagEntity_Table
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.database.tables.TagSensorReading_Table
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.HumidityConverter

class TagRepository(
    private val preferences: Preferences,
    private val context: Context
) {

    fun getAllTags(isFavorite: Boolean): List<RuuviTagEntity> =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.favorite.eq(isFavorite))
            .orderBy(RuuviTagEntity_Table.createDate, true)
            .queryList()

    fun getTagById(id: String): RuuviTagEntity? =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.id.eq(id))
            .querySingle()

    fun deleteTagsAndRelatives(tag: RuuviTagEntity) {
        SQLite.delete(Alarm::class.java)
            .where(Alarm_Table.ruuviTagId.eq(tag.id))
            .execute()

        SQLite.delete(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(tag.id))
            .execute()

        tag.delete()
    }

    fun getTagReadings(tagId: String): List<TagSensorReading>? {
        return if (preferences.graphShowAllPoint) {
            TagSensorReading.getForTag(tagId, preferences.graphViewPeriod)
        } else {
            TagSensorReading.getForTagPruned(
                tagId,
                preferences.graphPointInterval,
                preferences.graphViewPeriod
            )
        }
    }

    fun getHumidityString(tag: RuuviTag): String {
        val humidityUnit = getHumidityUnit()
        val calculation = HumidityConverter(tag.temperature, tag.humidity / 100)

        return when (humidityUnit) {
            HumidityUnit.PERCENT -> context.getString(R.string.humidity_reading, tag.humidity)
            HumidityUnit.GM3 -> context.getString(R.string.humidity_absolute_reading, calculation.ah)
            HumidityUnit.DEW -> {
                when (getTemperatureUnit()) {
                    "K" -> context.getString(R.string.humidity_dew_reading, calculation.TdK) + " " + getTemperatureUnit()
                    "F" -> context.getString(R.string.humidity_dew_reading, calculation.TdF) + " °" + getTemperatureUnit()
                    else -> context.getString(R.string.humidity_dew_reading, calculation.Td) + " °" + getTemperatureUnit()
                }
            }
        }
    }

    //TODO REMOVE IT
    fun getTemperatureUnit(): String =
        preferences.temperatureUnit

    private fun getHumidityUnit(): HumidityUnit =
        preferences.humidityUnit

    fun updateTag(tag: RuuviTagEntity) {
        tag.update()
    }

    fun saveTag(@NonNull tag: RuuviTagEntity) {
        tag.save()
    }
}