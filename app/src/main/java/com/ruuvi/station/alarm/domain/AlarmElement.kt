package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import java.util.*

data class AlarmElement(
    var type: AlarmType,
    var isEnabled: Boolean,
    var min: Int,
    var max: Int,
    var customDescription: String = "",
    var mutedTill: Date? = null,
    val gap: Int = 1
) {
    var low: Int
    var high: Int
     var alarm: Alarm? = null

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
}