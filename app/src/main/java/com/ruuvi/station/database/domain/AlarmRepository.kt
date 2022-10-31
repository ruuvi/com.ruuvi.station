package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.kotlinextensions.update
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.Alarm_Table
import java.util.*

class AlarmRepository {
    fun getForSensor(sensorId: String): List<Alarm> =
        SQLite
            .select()
            .from(Alarm::class.java)
            .where(Alarm_Table.ruuviTagId.eq(sensorId))
            .queryList()

    fun getById(alarmId: Int) : Alarm? =
        SQLite
            .select()
            .from(Alarm::class.java)
            .where(Alarm_Table.id.eq(alarmId))
            .querySingle()

    fun disableAlarm(alarmId: Int) {
        val alarm = getById(alarmId)
        alarm?.let {
            disableAlarm(it)
        }
    }

    fun getActiveByType(type: Int) : List<Alarm> =
        SQLite
            .select()
            .from(Alarm::class.java)
            .where(Alarm_Table.type.eq(type))
            .and(Alarm_Table.enabled.eq(true))
            .queryList()

    fun disableAlarm(alarm: Alarm) {
        alarm.enabled = false
        alarm.update()
    }

    fun muteAlarm(alarmId: Int, mutedTill: Date) {
        val alarm = getById(alarmId)
        alarm?.let {
            it.mutedTill = mutedTill
            it.update()
        }
    }

    fun upsertAlarm(
        sensorId: String,
        min: Double,
        max: Double,
        type: Int,
        enabled: Boolean,
        description: String
    ): Alarm {
        var alarm = getForSensor(sensorId).firstOrNull { it.type == type }
        if (alarm == null) {
            alarm = Alarm()
        }

        val min = if (type == Alarm.MOVEMENT) 0.0 else min
        val max = if (type == Alarm.MOVEMENT) 0.0 else max

        alarm.ruuviTagId = sensorId
        alarm.enabled = enabled
        alarm.min = min
        alarm.max = max
        alarm.type = type
        alarm.customDescription = description
        alarm.save()
        return alarm
    }
}