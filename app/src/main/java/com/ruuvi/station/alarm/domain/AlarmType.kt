package com.ruuvi.station.alarm.domain

enum class AlarmType(val value: Int, val networkCode: String?, val possibleRange: IntRange, val extraRange: IntRange) {
    TEMPERATURE(0, "temperature", -40 .. 85, -55..150),
    HUMIDITY(1, "humidity", 0 .. 100, 0 .. 100),
    PRESSURE(2, "pressure", 50000 .. 115500, 50000 .. 115500),
    RSSI(3, "signal", -105 .. 0, -105 .. 0),
    MOVEMENT(4, "movement", 0 .. 0, 0 .. 0),
    OFFLINE(5, "offline", 120..86400, 120..86400),
    CO2(6, "co2", 350..2500, 350..2500),
    PM1(7, "pm10", 0..250, 0..250),
    PM25(8, "pm25", 0..250, 0..250),
    PM4(9, "pm40", 0..250, 0..250),
    PM10(10, "pm100", 0..250, 0..250),
    SOUND(11, "sound", 0..127, 0..127),
    LUMINOSITY(12, "luminosity", 0..10000, 0..10000),
    VOC(13, "voc", 0..500, 0..500),
    NOX(14, "nox", 0..500, 0..500);

    fun valueInRange(value: Double): Boolean = value >= extraRange.first && value <= extraRange.last

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }

        fun getByDbCode(code: Int): AlarmType = values().firstOrNull { it.value == code } ?: TEMPERATURE
    }
}