package com.ruuvi.station.graph.model

import com.ruuvi.station.R

enum class ChartSensorType (val captionTemplate: Int) {
    TEMPERATURE (R.string.temperature_with_unit),
    HUMIDITY (R.string.humidity_with_unit),
    PRESSURE (R.string.pressure_with_unit),
    BATTERY (R.string.battery_voltage),
    ACCELERATION (R.string.acceleration_x),
    RSSI (R.string.signal_strength_rssi),
    MOVEMENTS (R.string.movement_counter),
    CO2 (R.string.co2_with_unit),
    VOC (R.string.voc_with_unit),
    NOX (R.string.nox_with_unit),
    PM25 (R.string.pm25_with_unit),
    LUMINOSITY (R.string.luminosity_with_unit),
    SOUND (R.string.sound_with_unit),
    AQI (R.string.aqi)
}