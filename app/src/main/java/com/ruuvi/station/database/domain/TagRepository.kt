package com.ruuvi.station.database.domain

import androidx.annotation.NonNull
import com.raizlabs.android.dbflow.config.DatabaseDefinition
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.*
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagConverter
import timber.log.Timber
import java.util.*
import java.text.Collator

class TagRepository(
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val database: DatabaseDefinition,
    private val tagConverter: TagConverter
) {
    fun getAllTags(isFavorite: Boolean): List<RuuviTagEntity> =
        SQLite.select(*RuuviTagEntity.queryFields)
            .from(RuuviTagEntity::class.java)
            .leftOuterJoin(SensorSettings::class.java)
            .on(SensorSettings_Table.id.withTable().eq(RuuviTagEntity_Table.id.withTable()))
            .where(if (isFavorite) SensorSettings_Table.id.withTable().isNotNull else SensorSettings_Table.id.withTable().isNull)
            .queryList()

    fun getTagById(id: String): RuuviTagEntity? =
        SQLite.select()
            .from(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.id.eq(id))
            .querySingle()

    fun activateSensor(reading: TagSensorReading) {
        reading.ruuviTagId?.let { sensorId ->
            var tagEntry = getTagById(sensorId)
            if (tagEntry == null) {
                tagEntry = RuuviTagEntity(reading)
                tagEntry.favorite = true
                tagEntry.insert()
            } else {
                tagEntry.favorite = true
                val previousUpdate = tagEntry.updateAt
                if (previousUpdate == null ||  previousUpdate < reading.createdAt) {
                    tagEntry.updateData(reading)
                    tagEntry.connectable = false
                }
                tagEntry.update()
            }
        }
    }

    fun getFavoriteSensors(): List<RuuviTag> {
        return SQLite
            .select(*FavouriteSensorQuery.queryFields)
            .from(SensorSettings::class.java)
            .leftOuterJoin(RuuviTagEntity::class.java)
            .on(SensorSettings_Table.id.withTable().eq(RuuviTagEntity_Table.id.withTable()))
            .queryCustomList(FavouriteSensorQuery::class.java)
            .map { tagConverter.fromDatabase(it) }
            .sortedWith( compareBy(Collator.getInstance()) {it.displayName} )
    }

    fun getFavoriteSensorById(id: String): RuuviTag? {
        val queryResult = SQLite
            .select(*FavouriteSensorQuery.queryFields)
            .from(SensorSettings::class.java)
            .leftOuterJoin(RuuviTagEntity::class.java)
            .on(SensorSettings_Table.id.withTable().eq(RuuviTagEntity_Table.id.withTable()))
            .where(SensorSettings_Table.id.withTable().eq(id))
            .orderBy(SensorSettings_Table.createDate.withTable(), true)
            .queryCustomSingle(FavouriteSensorQuery::class.java)
        return if (queryResult != null) tagConverter.fromDatabase(queryResult) else null
    }

    fun deleteSensorAndRelatives(sensorId: String) {
        SQLite.delete(Alarm::class.java)
            .where(Alarm_Table.ruuviTagId.eq(sensorId))
            .execute()

        SQLite.delete(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(sensorId))
            .execute()

        SQLite.delete(SensorSettings::class.java)
            .where(SensorSettings_Table.id.eq(sensorId))
            .execute()

        SQLite.delete(SensorsShareList::class.java)
            .where(SensorsShareList_Table.sensorId.eq(sensorId))
            .execute()

        SQLite.delete(RuuviTagEntity::class.java)
            .where(RuuviTagEntity_Table.id.eq(sensorId))
            .execute()
    }

    fun updateTag(tag: RuuviTagEntity) {
        tag.update()
    }

    fun saveTag(@NonNull tag: RuuviTagEntity) {
        tag.save()
    }

    fun makeSensorFavorite(sensor: RuuviTagEntity) {
        val transaction = database.beginTransactionAsync {
            sensor.id?.let { sensorId ->
                sensor.favorite = true
                val sensorSettings = SensorSettings(id = sensorId, createDate = Date(), name = sensor.displayName())
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