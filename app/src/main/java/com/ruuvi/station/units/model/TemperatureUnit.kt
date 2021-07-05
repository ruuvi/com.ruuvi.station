package com.ruuvi.station.units.model

import com.ruuvi.station.R

enum class TemperatureUnit (val code: String, val title: Int, val unit: Int) {
    CELSIUS("C", R.string.temperature_celsius_name, R.string.temperature_celsius_unit),
    FAHRENHEIT("F", R.string.temperature_fahrenheit_name, R.string.temperature_fahrenheit_unit),
    KELVIN("K", R.string.temperature_kelvin_name, R.string.temperature_kelvin_unit);

    companion object {
        fun getByCode(code: String) = values().firstOrNull{it.code == code}
    }
}