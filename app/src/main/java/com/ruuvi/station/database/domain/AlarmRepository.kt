package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.kotlinextensions.save
import com.raizlabs.android.dbflow.kotlinextensions.update
import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.Alarm_Table
import timber.log.Timber
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
        low: Int,
        high: Int,
        type: Int,
        enabled: Boolean,
        description: String
    ): Alarm {
        Timber.d("upsertAlarm $sensorId $low-$high ($type) - $enabled")
        var alarm = getForSensor(sensorId).firstOrNull { it.type == type }
        if (alarm == null) {
            alarm = Alarm()
        }
        alarm.ruuviTagId = sensorId
        alarm.enabled = enabled
        alarm.low = low
        alarm.high = high
        alarm.type = type
        alarm.customDescription = description
        alarm.save()
        return alarm
    }

    fun saveAlarmElement(alarmElement : AlarmElement) {
        with(alarmElement) {
            if (alarm == null) {
                alarm = Alarm(
                    ruuviTagId = sensorId,
                    low = low,
                    high = high,
                    type = type.value,
                    customDescription = customDescription,
                    mutedTill = mutedTill,
                    enabled = isEnabled
                )
            } else {
                alarm?.let {
                    it.enabled = isEnabled
                    it.low = low
                    it.high = high
                    it.customDescription = customDescription
                    it.mutedTill = mutedTill
                }
            }
            alarm?.save()
        }
    }
}