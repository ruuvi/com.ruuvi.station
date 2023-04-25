package com.ruuvi.station.widgets.data

import com.ruuvi.station.R

enum class WidgetType(val code: Int, val titleResId: Int) {
    TEMPERATURE(1, R.string.temperature),
    HUMIDITY(2, R.string.humidity),
    PRESSURE(3, R.string.pressure),
    MOVEMENT(4, R.string.movement_counter),
    VOLTAGE(5, R.string.battery_voltage),
    SIGNAL_STRENGTH(6, R.string.signal_strength_rssi),
    ACCELERATION_X(7, R.string.acceleration_x),
    ACCELERATION_Y(8, R.string.acceleration_y),
    ACCELERATION_Z(9, R.string.acceleration_z);

    companion object {
        fun getByCode(code: Int): WidgetType = values().firstOrNull{it.code == code} ?: TEMPERATURE
    }
}