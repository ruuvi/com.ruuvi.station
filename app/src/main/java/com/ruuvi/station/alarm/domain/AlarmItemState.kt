package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import java.util.*

data class AlarmItemState(
    val sensorId: String,
    var type: AlarmType,
    var isEnabled: Boolean,
    var low: Int,
    var high: Int,
    var customDescription: String = "",
    var mutedTill: Date? = null,
) {
    constructor(alarm: Alarm) : this(
        sensorId = alarm.ruuviTagId,
        type = alarm.alarmType,
        isEnabled = alarm.enabled,
        low = alarm.low,
        high = alarm.high,
        customDescription = alarm.customDescription,
        mutedTill = alarm.mutedTill
    ) {
        normalizeValues()
    }

    constructor(sensorId: String, alarmType: AlarmType) : this(
        sensorId = sensorId,
        type = alarmType,
        isEnabled = false,
        low = alarmType.possibleRange.first,
        high = alarmType.possibleRange.last
    )

    fun getPossibleRange() = type.possibleRange

    fun normalizeValues() {
        val min = getPossibleRange().first
        val max = getPossibleRange().last
        if (low < min) low = min
        if (low >= max) low = max - 1
        if (high > max) high = max
        if (high < min) high = min + 1
        if (low > high) {
            low = high.also { high = low }
        }
    }
}
