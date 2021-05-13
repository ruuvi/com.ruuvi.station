package com.ruuvi.station.database.domain

import androidx.annotation.NonNull
import com.raizlabs.android.dbflow.config.DatabaseDefinition
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.*
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagConverter
import timber.log.Timber
import java.util.*

class TagRepository(
    private val preferences: Preferences,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val database: DatabaseDefinition,
    private val tagConverter: TagConverter
) {
    fun getAllTags(isFavorite: Boolean): List<RuuviTagEntity> =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.favorite.eq(isFavorite))
            //.orderBy(RuuviTagEntity_Table.createDate, true)
            .queryList()

    fun getTagById(id: String): RuuviTagEntity? =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.id.eq(id))
            .querySingle()

    fun getFavoriteSensors(): List<RuuviTag> {
        return SQLite
            .select(*FavouriteSensorQuery.queryFields)
            .from(SensorSettings::class.java)
            .innerJoin(RuuviTagEntity::class.java)
            .on(SensorSettings_Table.id.withTable().eq(RuuviTagEntity_Table.id.withTable()))
            .orderBy(SensorSettings_Table.createDate.withTable(), true)
            .queryCustomList(FavouriteSensorQuery::class.java)
            .map { tagConverter.fromDatabase(it) }
    }

    fun getFavoriteSensorById(id: String): RuuviTag? {
        val queryResult = SQLite
            .select(*FavouriteSensorQuery.queryFields)
            .from(SensorSettings::class.java)
            .innerJoin(RuuviTagEntity::class.java)
            .on(SensorSettings_Table.id.withTable().eq(RuuviTagEntity_Table.id.withTable()))
            .where(SensorSettings_Table.id.withTable().eq(id))
            .orderBy(SensorSettings_Table.createDate.withTable(), true)
            .queryCustomSingle(FavouriteSensorQuery::class.java)
        return if (queryResult != null) tagConverter.fromDatabase(queryResult) else null
    }

    fun deleteTagsAndRelatives(tag: RuuviTagEntity) {
        SQLite.delete(Alarm::class.java)
            .where(Alarm_Table.ruuviTagId.eq(tag.id))
            .execute()

        SQLite.delete(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(tag.id))
            .execute()

        SQLite.delete(SensorSettings::class.java)
            .where(SensorSettings_Table.id.eq(tag.id))
            .execute()

        tag.delete()
    }

    fun updateTag(tag: RuuviTagEntity) {
        tag.update()
    }

    fun saveTag(@NonNull tag: RuuviTagEntity) {
        tag.save()
    }

    fun getTagReadingsDate(tagId: String, since: Date): List<TagSensorReading>? {
        return SQLite.select()
            .from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(tagId))
            .and(TagSensorReading_Table.createdAt.greaterThanOrEq(since))
            .queryList()
    }

    fun makeSensorFavorite(sensor: RuuviTagEntity) {
        val transaction = database.beginTransactionAsync {
            sensor.id?.let { sensorId ->
                sensor.favorite = true
                val sensorSettings = SensorSettings(id = sensorId, createDate = Date())
                sensorSettingsRepository.setKindaRandomBackground(sensorSettings)
                sensorSettings.save(it)
                sensor.save(it)
            }
        }

        transaction
            .error { _, error ->
                Timber.e(error, "Failed to make sensor favourite: ${sensor.id}")
            }
            .build()
            .execute()
    }
}