package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.tables.Alarm
import java.util.*

data class AlarmElement(
    val sensorId: String,
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

    fun shouldBeSaved(): Boolean {
        if (alarm != null) {
            return isEnabled != alarm?.enabled || low != alarm?.low || high != alarm?.high || customDescription != alarm?.customDescription
        } else {
            return isEnabled
        }
    }

    companion object {
        fun getTemperatureAlarmElement(sensorId: String) = AlarmElement(
            sensorId,
            AlarmType.TEMPERATURE,
            false,
            -40,
            85
        )

        fun getHumidityAlarmElement(sensorId: String) = AlarmElement(
            sensorId,
            AlarmType.HUMIDITY,
            false,
            0,
            100
        )

        fun getPressureAlarmElement(sensorId: String) = AlarmElement(
            sensorId,
            AlarmType.PRESSURE,
            false,
            30000,
            110000
        )

        fun getRssiAlarmElement(sensorId: String) = AlarmElement(
            sensorId,
            AlarmType.RSSI,
            false,
            -105,
            0
        )

        fun getMovementAlarmElement(sensorId: String) = AlarmElement(
            sensorId,
            AlarmType.MOVEMENT,
            false,
            0,
            0
        )
    }
}