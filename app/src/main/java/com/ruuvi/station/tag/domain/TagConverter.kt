package com.ruuvi.station.tag.domain

import android.content.Context
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.units.domain.UnitsConverter

class TagConverter(
    private val context: Context,
    private val preferences: Preferences,
    private val unitsConverter: UnitsConverter
) {

    fun fromDatabase(entity: RuuviTagEntity, sensorSettings: SensorSettings): RuuviTag =
        RuuviTag(
            id = entity.id.orEmpty(),
            name = sensorSettings.name.orEmpty(),
            displayName = if (sensorSettings.name.isNullOrEmpty()) entity.id.toString() else sensorSettings.name.toString(),
            rssi = entity.rssi,
            temperature = entity.temperature,
            humidity = entity.humidity,
            pressure = entity.pressure,
            updatedAt = entity.updateAt,
            temperatureString = unitsConverter.getTemperatureString(entity.temperature),
            humidityString = unitsConverter.getHumidityString(entity.humidity, entity.temperature),
            pressureString = unitsConverter.getPressureString(entity.pressure),
            defaultBackground = sensorSettings.defaultBackground,
            userBackground = sensorSettings.userBackground,
            dataFormat = entity.dataFormat,
            connectable = entity.connectable,
            lastSync = sensorSettings.lastSync,
            networkLastSync = sensorSettings.networkLastSync
        )

    fun fromDatabase(entity: FavouriteSensorQuery): RuuviTag =
        RuuviTag(
            id = entity.id,
            name = entity.name.orEmpty(),
            displayName = if (entity.name.isNullOrEmpty()) entity.id else entity.name.orEmpty(),
            rssi = entity.rssi,
            temperature = entity.temperature,
            humidity = entity.humidity,
            pressure = entity.pressure,
            updatedAt = entity.updateAt,
            temperatureString = unitsConverter.getTemperatureString(entity.temperature),
            humidityString = unitsConverter.getHumidityString(entity.humidity, entity.temperature),
            pressureString = unitsConverter.getPressureString(entity.pressure),
            defaultBackground = entity.defaultBackground,
            userBackground = entity.userBackground,
            dataFormat = entity.dataFormat,
            connectable = entity.connectable,
            lastSync = entity.lastSync,
            networkLastSync = entity.networkLastSync
        )
}