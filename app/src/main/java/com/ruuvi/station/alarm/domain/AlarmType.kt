package com.ruuvi.station.alarm.domain

enum class AlarmType(
    val value: Int,
    val networkCode: String?,
    val possibleRange: ClosedFloatingPointRange<Double>,
    val extraRange: ClosedFloatingPointRange<Double>,
    val step: Double = 1.0,
    val roundPlaces: Int = 0
) {
    TEMPERATURE(0, "temperature", -40.0..85.0, -55.0..150.0),
    HUMIDITY(1, "humidity", 0.0..100.0, 0.0..100.0),
    PRESSURE(2, "pressure", 50000.0..115500.0, 50000.0..115500.0),
    RSSI(3, "signal", -105.0..0.0, -105.0..0.0),
    MOVEMENT(4, "movement", 0.0..0.0, 0.0..0.0),
    OFFLINE(5, "offline", 120.0..86400.0, 120.0..86400.0),
    CO2(6, "co2", 350.0..2500.0, 0.0..40000.0),
    PM10(7, "pm10", 0.0..250.0, 0.0..1000.0),
    PM25(8, "pm25", 0.0..250.0, 0.0..1000.0),
    PM40(9, "pm40", 0.0..250.0, 0.0..1000.0),
    PM100(10, "pm100", 0.0..250.0, 0.0..1000.0),
    SOUND(11, "sound", 0.0..127.0, 0.0..127.0),
    LUMINOSITY(12, "luminosity", 0.0..144284.0, 0.0..144284.0),
    VOC(13, "voc", 0.0..500.0, 0.0..500.0),
    NOX(14, "nox", 0.0..500.0, 0.0..500.0),
    AQI(15, "aqi", 0.0..100.0, 0.0..100.0),
    ABSOLUTE_HUMIDITY(16, "humidityAbsolute", 0.0..50.0, 0.0..50.0),
    DEW_POINT(17, "dewPoint", -45.0..85.0, -45.0..85.0),
    BATTERY_VOLTAGE(18, "battery", 1.8..3.6, 1.8..3.6, 0.1, roundPlaces = 1);


    fun valueInRange(value: Double): Boolean = value >= extraRange.start && value <= extraRange.endInclusive

    companion object {
        fun getByNetworkCode(networkCode: String): AlarmType? = values().firstOrNull { it.networkCode == networkCode }

        fun getByDbCode(code: Int): AlarmType = values().firstOrNull { it.value == code } ?: TEMPERATURE
    }
}