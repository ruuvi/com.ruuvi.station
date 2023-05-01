package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int, val networkCode: String?, val possibleRange: IntRange) {
    TEMPERATURE(0, "temperature", -40 .. 85),
    HUMIDITY(1, "humidity", 0 .. 100),
    PRESSURE(2, "pressure", 50000 .. 115500),
    RSSI(3, "signal", -105 .. 0),
    MOVEMENT(4, "movement", 0 .. 0);

    fun valueInRange(value: Double): Boolean = value >= possibleRange.first && value <= possibleRange.last

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }

        fun getByDbCode(code: Int): AlarmType = values().firstOrNull { it.value == code } ?: TEMPERATURE
    }
}