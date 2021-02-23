package com.ruuvi.station.units.domain

import com.ruuvi.station.util.Utils

class TemperatureConverter {
    companion object {
        fun celsiusToFahrenheit(celsius: Double): Double {
            return Utils.round(celsius * 1.8 + 32.0, 2)
        }

        fun celsiusToKelvin(celsius: Double): Double {
            return Utils.round(celsius + 273.15, 2)
        }
    }
}