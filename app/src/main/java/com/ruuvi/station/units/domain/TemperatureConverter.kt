package com.ruuvi.station.units.domain

import com.ruuvi.station.util.Utils

class TemperatureConverter {
    companion object {
        const val  fahrenheitMultiplier = 1.8

        fun celsiusToFahrenheit(celsius: Double): Double {
            return Utils.round(celsius * fahrenheitMultiplier + 32.0, 2)
        }

        fun celsiusToKelvin(celsius: Double): Double {
            return Utils.round(celsius + 273.15, 2)
        }

        fun fahrenheitToCelsius(fahrenheit: Double): Double {
            return Utils.round((fahrenheit-32.0) / fahrenheitMultiplier, 2)
        }

        fun kelvinToCelsius(kelvin: Double): Double {
            return Utils.round(kelvin - 273.15, 2)
        }
    }
}