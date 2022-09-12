package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import java.util.*

data class AlarmItemState(
    val sensorId: String,
    var type: AlarmType,
    var isEnabled: Boolean,
    var low: Float,
    var high: Float,
    var customDescription: String = "",
    var mutedTill: Date? = null,
) {
    constructor(alarm: Alarm) : this(
        sensorId = alarm.ruuviTagId,
        type = alarm.alarmType,
        isEnabled = alarm.enabled,
        low = alarm.low.toFloat(),
        high = alarm.high.toFloat(),
        customDescription = alarm.customDescription,
        mutedTill = alarm.mutedTill
    ) {
        normalizeValues()
    }

    constructor(sensorId: String, alarmType: AlarmType) : this(
        sensorId = sensorId,
        type = alarmType,
        isEnabled = false,
        low = alarmType.possibleRange.first.toFloat(),
        high = alarmType.possibleRange.last.toFloat()
    )

    fun getPossibleRange() = type.possibleRange

    fun normalizeValues() {
        val min = getPossibleRange().first.toFloat()
        val max = getPossibleRange().last.toFloat()
        if (low < min) low = min
        if (low >= max) low = max - 1
        if (high > max) high = max
        if (high < min) high = min + 1
        if (low > high) {
            low = high.also { high = low }
        }
    }
}
