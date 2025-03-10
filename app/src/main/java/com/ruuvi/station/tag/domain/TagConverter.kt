package com.ruuvi.station.tag.domain

import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI

class TagConverter(
    private val unitsConverter: UnitsConverter,
    private val movementConverter: MovementConverter
) {

    fun fromDatabase(entity: RuuviTagEntity, sensorSettings: SensorSettings): RuuviTag {
        val temperature = entity.temperature?.let { it + (sensorSettings.temperatureOffset ?: 0.0)}
        val humidity = entity.humidity?.let {it + (sensorSettings.humidityOffset ?: 0.0)}
        val pressure = entity.pressure?.let {it + (sensorSettings.pressureOffset ?: 0.0)}

        return RuuviTag(
            id = entity.id.orEmpty(),
            name = sensorSettings.name.orEmpty(),
            displayName = if (sensorSettings.name.isNullOrEmpty()) entity.id.toString() else sensorSettings.name.toString(),
            temperatureOffset = sensorSettings.temperatureOffset,
            humidityOffset = sensorSettings.humidityOffset,
            pressureOffset = sensorSettings.pressureOffset,
            defaultBackground = sensorSettings.defaultBackground,
            userBackground = sensorSettings.userBackground,
            networkBackground = sensorSettings.networkBackground,
            lastSync = sensorSettings.lastSync,
            networkLastSync = sensorSettings.networkLastSync,
            networkSensor = sensorSettings.networkSensor,
            owner = sensorSettings.owner,
            firmware = sensorSettings.firmware,
            subscriptionName = sensorSettings.subscriptionName,
            latestMeasurement = SensorMeasurements(
                aqi = unitsConverter.getAqiEnviromentValue(AQI.getAQI(
                    pm25 = entity.pm25,
                    co2 = entity.co2,
                    nox = entity.nox,
                    voc = entity.voc)
                ),
                temperatureValue = temperature?.let { unitsConverter.getTemperatureEnvironmentValue(it) },
                pressureValue = pressure?.let { unitsConverter.getPressureEnvironmentValue(it) },
                humidityValue = humidity?.let {
                    unitsConverter.getHumidityEnvironmentValue(
                        it,
                        temperature
                    )
                },
                movementValue = entity.movementCounter?.let {
                    movementConverter.getMovementEnvironmentValue(
                        it
                    )
                },
                rssiValue = unitsConverter.getSignalEnvironmentValue(entity.rssi),
                voltageValue = unitsConverter.getVoltageEnvironmentValue(entity.voltage),
                accelerationX = entity.accelX,
                accelerationY = entity.accelY,
                accelerationZ = entity.accelZ,
                txPower = entity.txPower,
                pm1 = entity.pm1?.let { unitsConverter.getPmEnvironmentValue(it) },
                pm25 = entity.pm25?.let { unitsConverter.getPmEnvironmentValue(it) },
                pm4 = entity.pm4?.let { unitsConverter.getPmEnvironmentValue(it) },
                pm10 = entity.pm10?.let { unitsConverter.getPmEnvironmentValue(it) },
                co2 = entity.co2?.let { unitsConverter.getCo2EnvironmentValue(it) },
                nox = entity.nox?.let { unitsConverter.getNoxEnvironmentValue(it) },
                voc = entity.voc?.let { unitsConverter.getVocEnvironmentValue(it) },
                luminosity = entity.luminosity?.let { unitsConverter.getLuminosityEnvironmentValue(it) },
                dBaAvg = entity.dBaAvg?.let { unitsConverter.getNoiseEnvironmentValue(it) },
                dBaPeak = entity.dBaPeak?.let { unitsConverter.getNoiseEnvironmentValue(it) },
                dataFormat = entity.dataFormat,
                measurementSequenceNumber = entity.measurementSequenceNumber,
                connectable = entity.connectable,
                updatedAt = entity.updateAt,
            )
        )
    }

    fun fromDatabase(entity: FavouriteSensorQuery): RuuviTag {
        val temperature = entity.temperature?.let { it + (entity.temperatureOffset ?: 0.0)}
        val humidity = entity.humidity?.let {it + (entity.humidityOffset ?: 0.0)}
        val pressure = entity.pressure?.let {it + (entity.pressureOffset ?: 0.0)}

        return RuuviTag(
            id = entity.id,
            name = entity.name.orEmpty(),
            displayName = if (entity.name.isNullOrEmpty()) entity.id else entity.name.orEmpty(),
            temperatureOffset = entity.temperatureOffset,
            humidityOffset = entity.humidityOffset,
            pressureOffset = entity.pressureOffset,
            defaultBackground = entity.defaultBackground,
            userBackground = entity.userBackground,
            networkBackground = entity.networkBackground,
            lastSync = entity.lastSync,
            networkLastSync = entity.networkLastSync,
            networkSensor = entity.networkSensor,
            owner = entity.owner,
            subscriptionName = entity.subscriptionName,
            firmware = entity.firmware,
            latestMeasurement = entity.latestId?.let {
                SensorMeasurements(
                    aqi = unitsConverter.getAqiEnviromentValue(AQI.getAQI(
                        pm25 = entity.pm25,
                        co2 = entity.co2,
                        nox = entity.nox,
                        voc = entity.voc)
                    ),
                    temperatureValue = temperature?.let {
                        unitsConverter.getTemperatureEnvironmentValue(it)
                    },
                    pressureValue = pressure?.let {
                        unitsConverter.getPressureEnvironmentValue(it)
                    },
                    humidityValue = humidity?.let {
                        unitsConverter.getHumidityEnvironmentValue(it, temperature)
                    },
                    movementValue = entity.movementCounter?.let {
                        movementConverter.getMovementEnvironmentValue(it)
                    },
                    voltageValue = unitsConverter.getVoltageEnvironmentValue(entity.voltage),
                    rssiValue = unitsConverter.getSignalEnvironmentValue(entity.rssi),
                    accelerationX = entity.accelX,
                    accelerationY = entity.accelY,
                    accelerationZ = entity.accelZ,
                    txPower = entity.txPower,
                    pm1 = entity.pm1?.let { unitsConverter.getPmEnvironmentValue(it) },
                    pm25 = entity.pm25?.let { unitsConverter.getPmEnvironmentValue(it) },
                    pm4 = entity.pm4?.let { unitsConverter.getPmEnvironmentValue(it) },
                    pm10 = entity.pm10?.let { unitsConverter.getPmEnvironmentValue(it) },
                    co2 = entity.co2?.let { unitsConverter.getCo2EnvironmentValue(it) },
                    nox = entity.nox?.let { unitsConverter.getNoxEnvironmentValue(it) },
                    voc = entity.voc?.let { unitsConverter.getVocEnvironmentValue(it) },
                    luminosity = entity.luminosity?.let { unitsConverter.getLuminosityEnvironmentValue(it) },
                    dBaAvg = entity.dBaAvg?.let { unitsConverter.getNoiseEnvironmentValue(it) },
                    dBaPeak = entity.dBaPeak?.let { unitsConverter.getNoiseEnvironmentValue(it) },
                    dataFormat = entity.dataFormat,
                    measurementSequenceNumber = entity.measurementSequenceNumber,
                    connectable = entity.connectable,
                    updatedAt = entity.updateAt,
                )
            }
        )
    }
}