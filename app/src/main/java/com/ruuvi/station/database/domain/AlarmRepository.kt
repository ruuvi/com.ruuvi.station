package com.ruuvi.station.database.domain

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
            it.enabled = false
            it.update()
        }
    }

    fun muteAlarm(alarmId: Int, mutedTill: Date) {
        val alarm = getById(alarmId)
        alarm?.let {
            it.mutedTill = mutedTill
            it.update()
        }
    }
}