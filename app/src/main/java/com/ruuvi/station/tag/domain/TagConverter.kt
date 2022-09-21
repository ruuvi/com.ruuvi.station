package com.ruuvi.station.tag.domain

import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter

class TagConverter(
    private val unitsConverter: UnitsConverter,
    private val movementConverter: MovementConverter
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
            movementCounter = entity.movementCounter,
            updatedAt = entity.updateAt,
            temperatureString = unitsConverter.getTemperatureString(entity.temperature),
            humidityString = unitsConverter.getHumidityString(entity.humidity, entity.temperature),
            pressureString = unitsConverter.getPressureString(entity.pressure),
            temperatureOffset = sensorSettings.temperatureOffset,
            humidityOffset = sensorSettings.humidityOffset,
            pressureOffset = sensorSettings.pressureOffset,
            movementCounterString = movementConverter.getMovementString(entity.movementCounter),
            defaultBackground = sensorSettings.defaultBackground,
            userBackground = sensorSettings.userBackground,
            dataFormat = entity.dataFormat,
            connectable = entity.connectable,
            lastSync = sensorSettings.lastSync,
            networkLastSync = sensorSettings.networkLastSync,
            networkSensor = sensorSettings.networkSensor,
            owner = sensorSettings.owner
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
            movementCounter = entity.movementCounter,
            temperatureString = unitsConverter.getTemperatureString(entity.temperature),
            humidityString = unitsConverter.getHumidityString(entity.humidity, entity.temperature),
            pressureString = unitsConverter.getPressureString(entity.pressure),
            temperatureOffset = entity.temperatureOffset,
            humidityOffset = entity.humidityOffset,
            pressureOffset = entity.pressureOffset,
            movementCounterString = movementConverter.getMovementString(entity.movementCounter),
            defaultBackground = entity.defaultBackground,
            userBackground = entity.userBackground,
            dataFormat = entity.dataFormat,
            connectable = entity.connectable,
            lastSync = entity.lastSync,
            networkLastSync = entity.networkLastSync,
            networkSensor = entity.networkSensor,
            owner = entity.owner
        )
}