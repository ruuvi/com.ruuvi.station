package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int, val networkCode: String?) {
    TEMPERATURE(0, "temperature"),
    HUMIDITY(1, "humidity"),
    PRESSURE(2, "pressure"),
    RSSI(3, null),
    MOVEMENT(4, null);

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }
    }
}