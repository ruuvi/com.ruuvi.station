package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int, val networkCode: String?, val range: IntRange) {
    TEMPERATURE(0, "temperature", -40 .. 85),
    HUMIDITY(1, "humidity", 0 .. 100),
    PRESSURE(2, "pressure", 30000 .. 110000),
    RSSI(3, "signal", -105 .. 0),
    MOVEMENT(4, "movement", 0 .. 0);

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }

        fun getByDbCode(code: Int): AlarmType = values().firstOrNull { it.value == code } ?: TEMPERATURE
    }
}