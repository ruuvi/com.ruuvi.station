package com.ruuvi.station.units.domain

import com.ruuvi.station.util.extensions.round

class TemperatureConverter {
    companion object {
        const val fahrenheitMultiplier = 1.8
        const val fahrenheitAddition = 32.0
        const val kelvinAddition = 273.15

        fun celsiusToFahrenheit(celsius: Double): Double {
            return (celsius * fahrenheitMultiplier + fahrenheitAddition).round(2)
        }

        fun celsiusToKelvin(celsius: Double): Double {
            return (celsius + kelvinAddition).round(2)
        }

        fun fahrenheitToCelsius(fahrenheit: Double): Double {
            return ((fahrenheit - fahrenheitAddition) / fahrenheitMultiplier).round(4)
        }

        fun kelvinToCelsius(kelvin: Double): Double {
            return (kelvin - kelvinAddition).round(2)
        }
    }
}