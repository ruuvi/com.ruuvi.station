package com.ruuvi.station.graph.model

import com.ruuvi.station.R

enum class ChartSensorType (val captionTemplate: Int) {
    TEMPERATURE (R.string.temperature_with_unit),
    HUMIDITY (R.string.humidity_with_unit),
    PRESSURE (R.string.pressure_with_unit),
    BATTERY (R.string.battery_voltage),
    ACCELERATION (R.string.acceleration_x),
    RSSI (R.string.signal_strength_rssi),
    MOVEMENTS (R.string.movement_counter)
}