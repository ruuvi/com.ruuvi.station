package com.ruuvi.station.tag.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.HumidityConverter

class TagConverter(
    private val context: Context,
    private val preferences: Preferences,
    private val unitsConverter: UnitsConverter
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
            temperatureString = unitsConverter.getTemperatureString(entity.temperature),
            humidityString = getHumidityString(entity),
            defaultBackground = entity.defaultBackground,
            userBackground = entity.userBackground,
            dataFormat = entity.dataFormat
        )

    private fun getHumidityString(tag: RuuviTagEntity): String {
        val humidityUnit = getHumidityUnit()
        val calculation = HumidityConverter(tag.temperature, tag.humidity / 100)

        return when (humidityUnit) {
            HumidityUnit.PERCENT -> context.getString(R.string.humidity_reading, tag.humidity)
            HumidityUnit.GM3 -> context.getString(R.string.humidity_absolute_reading, calculation.ah)
            HumidityUnit.DEW -> {
                when (unitsConverter.getTemperatureUnit()) {
                    "K" -> context.getString(R.string.humidity_dew_reading, calculation.TdK) + " " + unitsConverter.getTemperatureUnitString()
                    "F" -> context.getString(R.string.humidity_dew_reading, calculation.TdF) + " " + unitsConverter.getTemperatureUnitString()
                    else -> context.getString(R.string.humidity_dew_reading, calculation.Td) + " " + unitsConverter.getTemperatureUnitString()
                }
            }
        }
    }

    private fun getHumidityUnit(): HumidityUnit =
        preferences.humidityUnit
}