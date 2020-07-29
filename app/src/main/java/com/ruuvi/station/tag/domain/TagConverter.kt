package com.ruuvi.station.tag.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.util.Humidity
import com.ruuvi.station.util.Utils

class TagConverter(
    private val context: Context,
    private val preferences: Preferences
) {

    fun fromDatabase(entity: RuuviTagEntity): RuuviTag =
        RuuviTag(
            id = entity.id.orEmpty(),
            name = entity.name.orEmpty(),
            displayName = entity.name ?: entity.id.toString(),
            rssi = entity.rssi,
            temperature = entity.temperature,
            humidity = entity.humidity,
            pressure = entity.pressure,
            updatedAt = entity.updateAt,
            temperatureString = getTemperatureString(entity),
            humidityString = getHumidityString(entity)
        )

    private fun getHumidityString(tag: RuuviTagEntity): String {
        val humidityUnit = getHumidityUnit()
        val calculation = Humidity(tag.temperature, tag.humidity / 100)

        return when (humidityUnit) {
            HumidityUnit.PERCENT -> context.getString(R.string.humidity_reading, tag.humidity)
            HumidityUnit.GM3 -> context.getString(R.string.humidity_absolute_reading, calculation.ah)
            HumidityUnit.DEW -> {
                when (getTemperatureUnit()) {
                    "K" -> context.getString(R.string.humidity_dew_reading, calculation.TdK) + " " + getTemperatureUnit()
                    "F" -> context.getString(R.string.humidity_dew_reading, calculation.TdF) + " °" + getTemperatureUnit()
                    else -> context.getString(R.string.humidity_dew_reading, calculation.Td) + " °" + getTemperatureUnit()
                }
            }
        }
    }

    private fun getTemperatureString(tag: RuuviTagEntity): String =
        when (getTemperatureUnit()) {
            "C" -> context.getString(R.string.temperature_reading, tag.temperature) + "°" + getTemperatureUnit()
            "K" -> context.getString(R.string.temperature_reading, getKelvin(tag)) + getTemperatureUnit()
            "F" -> context.getString(R.string.temperature_reading, getFahrenheit(tag)) + "°" + getTemperatureUnit()
            else -> "Error"
        }

    private fun getTemperatureUnit(): String =
        preferences.temperatureUnit

    private fun getFahrenheit(tag: RuuviTagEntity): Double =
        Utils.celciusToFahrenheit(tag.temperature)

    private fun getKelvin(tag: RuuviTagEntity): Double =
        Utils.celsiusToKelvin(tag.temperature)

    private fun getHumidityUnit(): HumidityUnit =
        preferences.humidityUnit
}