package com.ruuvi.station.units.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity

class UnitsConverter (
        private val context: Context,
        private val preferences: Preferences
) {
    fun getTemperatureUnit() = preferences.temperatureUnit

    fun getTemperatureUnitString(): String {
        return when (getTemperatureUnit()) {
            "C" -> context.getString(R.string.temperature_unit_c)
            "K" -> context.getString(R.string.temperature_unit_k)
            "F" -> context.getString(R.string.temperature_unit_f)
            else -> throw IllegalArgumentException()
        }
    }

    fun getTemperatureValue(temperatureCelsius: Double): Double {
        return when (getTemperatureUnit()) {
            "C" -> temperatureCelsius
            "K" -> TemperatureConverter.celsiusToKelvin(temperatureCelsius)
            "F" -> TemperatureConverter.celsiusToFahrenheit(temperatureCelsius)
            else -> throw IllegalArgumentException()
        }
    }

    fun getTemperatureValue(tag: RuuviTagEntity): Double = getTemperatureValue(tag.temperature)

    fun getTemperatureString(tag: RuuviTagEntity): String =
            getTemperatureString(tag.temperature)

    fun getTemperatureString(temperature: Double): String =
            context.getString(R.string.temperature_reading, getTemperatureValue(temperature)) + getTemperatureUnitString()

    fun getTemperatureStringWithoutUnit(temperature: Double): String =
        context.getString(R.string.temperature_reading, getTemperatureValue(temperature))
}