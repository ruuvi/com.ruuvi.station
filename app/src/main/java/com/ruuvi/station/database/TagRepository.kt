package com.ruuvi.station.database

import androidx.annotation.NonNull
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.Alarm_Table
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.RuuviTagEntity_Table
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.database.tables.TagSensorReading_Table
import java.util.*

class TagRepository(
    private val preferences: Preferences
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
            TagSensorReading.getForTag(tagId, preferences.graphViewPeriodDays)
        } else {
            TagSensorReading.getForTagPruned(
                tagId,
                preferences.graphPointInterval,
                preferences.graphViewPeriodDays
            )
        }
    }

    fun updateTag(tag: RuuviTagEntity) {
        tag.update()
    }

    fun saveTag(@NonNull tag: RuuviTagEntity) {
        tag.save()
    }

    fun getLatestForTag(id: String, limit: Int): List<TagSensorReading> {
        return SQLite.select()
            .from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(id))
            .orderBy(TagSensorReading_Table.createdAt, false)
            .limit(limit)
            .queryList()
    }

    fun getTagReadingsDate(tagId: String, since: Date): List<TagSensorReading>? {
        return SQLite.select()
            .from(TagSensorReading::class.java)
            .where(TagSensorReading_Table.ruuviTagId.eq(tagId))
            .and(TagSensorReading_Table.createdAt.greaterThanOrEq(since))
            .queryList()
    }

    fun updateTagName(tagId: String, tagName: String?) {
        SQLite.update(RuuviTagEntity::class.java)
            .set(RuuviTagEntity_Table.name.eq(tagName))
            .where(RuuviTagEntity_Table.id.eq(tagId))
            .async()
            .execute()
    }

    fun updateTagBackground(tagId: String, userBackground: String?, defaultBackground: Int?) {
        SQLite.update(RuuviTagEntity::class.java)
            .set(
                RuuviTagEntity_Table.userBackground.eq(userBackground),
                RuuviTagEntity_Table.defaultBackground.eq(defaultBackground)
            )
            .where(RuuviTagEntity_Table.id.eq(tagId))
            .async()
            .execute()
    }
}