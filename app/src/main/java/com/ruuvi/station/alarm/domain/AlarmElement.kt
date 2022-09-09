package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import java.util.*

data class AlarmElement(
    val sensorId: String,
    var type: AlarmType,
    var isEnabled: Boolean,
    var customDescription: String = "",
    var mutedTill: Date? = null,
) {
    var low: Int
    var high: Int
    var alarm: Alarm? = null

    val min = type.possibleRange.first
    val max = type.possibleRange.last

    init {
        low = min
        high = max
    }

    fun normalizeValues() {
        if (low < min) low = min
        if (low >= max) low = max - gap
        if (high > max) high = max
        if (high < min) high = min + gap
        if (low > high) {
            low = high.also { high = low }
        }
    }

    fun shouldBeSaved(): Boolean {
        if (alarm != null) {
            return isEnabled != alarm?.enabled || low != alarm?.low || high != alarm?.high || customDescription != alarm?.customDescription
        } else {
            return isEnabled
        }
    }

    companion object {
        const val gap: Int = 1

        fun getDbAlarmElement(alarm: Alarm): AlarmElement {
            val alarmElement = AlarmElement(
                sensorId = alarm.ruuviTagId,
                type = alarm.alarmType,
                isEnabled = alarm.enabled,
                customDescription = alarm.customDescription,
                mutedTill = alarm.mutedTill
            )
            alarmElement.high = alarm.high
            alarmElement.low = alarm.low
            alarmElement.alarm = alarm
            alarmElement.normalizeValues()
            return alarmElement
        }

        fun getDefaultAlarmElement(sensorId: String, alarmType: AlarmType): AlarmElement {
            return AlarmElement(
                sensorId = sensorId,
                type = alarmType,
                isEnabled = false
            )
        }
    }
}