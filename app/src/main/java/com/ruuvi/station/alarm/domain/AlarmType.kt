package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int, val networkCode: String?) {
    TEMPERATURE(0, "temperature"),
    HUMIDITY(1, "humidity"),
    PRESSURE(2, "pressure"),
    RSSI(3, "signal"),
    MOVEMENT(4, "movement");

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }
    }
}