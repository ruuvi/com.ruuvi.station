package com.ruuvi.station.widgets.data

enum class WidgetType(val code: Int) {
    TEMPERATURE(1),
    HUMIDITY(2),
    PRESSURE(3),
    MOVEMENT(4),
    VOLTAGE(5),
    SIGNAL_STRENGTH(6),
    ACCELERATION_X(7),
    ACCELERATION_Y(8),
    ACCELERATION_Z(9);

    companion object {
        fun getByCode(code: Int): WidgetType = values().firstOrNull{it.code == code} ?: TEMPERATURE
    }
}