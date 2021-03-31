package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int) {
    TEMPERATURE(0),
    HUMIDITY(1),
    PRESSURE(2),
    RSSI(3),
    MOVEMENT(4)
}