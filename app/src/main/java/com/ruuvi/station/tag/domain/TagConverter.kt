package com.ruuvi.station.tag.domain

import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType.*
import timber.log.Timber

class TagConverter(
    private val unitsConverter: UnitsConverter,
    private val movementConverter: MovementConverter,
    private val visibleMeasurementsOrderInteractor: VisibleMeasurementsOrderInteractor,
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
            defaultDisplayOrder = sensorSettings.defaultDisplayOrder,
            displayOrder = listOf(),
            possibleDisplayOptions = listOf(),
            valuesToDisplay = listOf(),
            latestMeasurement = SensorMeasurements(
                aqi = unitsConverter.getAqiEnviromentValue(AQI.getAQI(
                    pm25 = entity.pm25,
                    co2 = entity.co2)
                ),
                temperature = temperature?.let { unitsConverter.getTemperatureEnvironmentValue(it) },
                pressure = pressure?.let { unitsConverter.getPressureEnvironmentValue(it) },
                humidity = humidity?.let {
                    unitsConverter.getHumidityEnvironmentValue(
                        it,
                        temperature
                    )
                },
                movement = entity.movementCounter?.let {
                    movementConverter.getMovementEnvironmentValue(
                        it
                    )
                },
                rssi = unitsConverter.getSignalEnvironmentValue(entity.rssi),
                voltage = unitsConverter.getVoltageEnvironmentValue(entity.voltage),
                accelerationX = entity.accelX,
                accelerationY = entity.accelY,
                accelerationZ = entity.accelZ,
                txPower = entity.txPower,
                pm10 = entity.pm1?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM10) },
                pm25 = entity.pm25?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM25) },
                pm40 = entity.pm4?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM40) },
                pm100 = entity.pm10?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM100) },
                co2 = entity.co2?.let { unitsConverter.getCo2EnvironmentValue(it) },
                nox = entity.nox?.let { unitsConverter.getNoxEnvironmentValue(it) },
                voc = entity.voc?.let { unitsConverter.getVocEnvironmentValue(it) },
                luminosity = entity.luminosity?.let { unitsConverter.getLuminosityEnvironmentValue(it) },
                dBaAvg = entity.dBaAvg?.let { unitsConverter.getSoundEnvironmentValue(it, SoundAvg.SoundDba) },
                dBaPeak = entity.dBaPeak?.let { unitsConverter.getSoundEnvironmentValue(it, SoundPeak.SoundDba) },
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

        val defaultDisplayOrder = visibleMeasurementsOrderInteractor.getDefaultDisplayOrder(entity)

        val userDefinedDisplayOrder = visibleMeasurementsOrderInteractor.getUserDefinedOrder(
            displayOrder = entity.displayOrder,
            defaultOrder = defaultDisplayOrder
        )

        val displayOrder =
            if (entity.defaultDisplayOrder) {
                defaultDisplayOrder
            } else {
                userDefinedDisplayOrder
            }

        val possibleOptions = visibleMeasurementsOrderInteractor.getPossibleDisplayOptions(entity)

        val possibleOptionsFiltered = possibleOptions
            .filter { it !in displayOrder }
            .filterNotNull()

        Timber.d("DISPLAY ORDER CHECK possible = $possibleOptions display = $displayOrder filtered = $possibleOptionsFiltered")

        val valuesToDisplay = mutableListOf<EnvironmentValue>()
        for (unit in displayOrder) {
            when (unit) {
                is TemperatureUnit -> temperature?.let {
                    valuesToDisplay.add(
                        unitsConverter.getTemperatureEnvironmentValue(temperature, unit)
                    )
                }
                is HumidityUnit -> humidity?.let {
                    val humidityEnvValue = unitsConverter.getHumidityEnvironmentValue(humidity, temperature, unit)
                    humidityEnvValue?.let {
                        valuesToDisplay.add(it)
                    }
                }
                is PressureUnit -> pressure?.let {
                    valuesToDisplay.add(
                        unitsConverter.getPressureEnvironmentValue(it, unit)
                    )
                }
                Acceleration.GForceX -> {
                    valuesToDisplay.add(
                        unitsConverter.getAccelerationValue(entity.accelX, Acceleration.GForceX)
                    )
                }
                Acceleration.GForceY -> {
                    valuesToDisplay.add(
                        unitsConverter.getAccelerationValue(entity.accelY, Acceleration.GForceY)
                    )
                }
                Acceleration.GForceZ -> {
                    valuesToDisplay.add(
                        unitsConverter.getAccelerationValue(entity.accelZ, Acceleration.GForceZ)
                    )
                }
                BatteryVoltageUnit.Volt -> {
                    valuesToDisplay.add(
                        unitsConverter.getVoltageEnvironmentValue(entity.voltage)
                    )
                }
                MovementUnit.MovementsCount -> {
                    entity.movementCounter?.let {
                        valuesToDisplay.add(
                            movementConverter.getMovementEnvironmentValue(it)
                        )
                    }
                }
                SignalStrengthUnit.SignalDbm -> {
                    valuesToDisplay.add(
                        unitsConverter.getSignalEnvironmentValue(entity.rssi)
                    )
                }
                AirQuality.AqiIndex -> {
                    valuesToDisplay.add(
                        unitsConverter.getAqiEnviromentValue(AQI.getAQI(
                            pm25 = entity.pm25,
                            co2 = entity.co2)
                        )
                    )
                }
                Luminosity.Lux -> {
                    entity.luminosity?.let {
                        valuesToDisplay.add(unitsConverter.getLuminosityEnvironmentValue(it))
                    }
                }
                SoundAvg.SoundDba -> {
                    entity.dBaAvg?.let {
                        valuesToDisplay.add(unitsConverter.getSoundEnvironmentValue(it, SoundAvg.SoundDba))
                    }
                }
                SoundPeak.SoundDba -> {
                    entity.dBaPeak?.let {
                        valuesToDisplay.add(unitsConverter.getSoundEnvironmentValue(it, SoundPeak.SoundDba))
                    }
                }
                CO2.Ppm -> {
                    entity.co2?.let {
                        valuesToDisplay.add(unitsConverter.getCo2EnvironmentValue(it))
                    }
                }
                VOC.VocIndex -> {
                    entity.voc?.let {
                        valuesToDisplay.add(unitsConverter.getVocEnvironmentValue(it))
                    }
                }
                NOX.NoxIndex -> {
                    entity.nox?.let {
                        valuesToDisplay.add(unitsConverter.getNoxEnvironmentValue(it))
                    }
                }
                PM.PM10 -> {
                    entity.pm1?.let {
                        valuesToDisplay.add(unitsConverter.getPmEnvironmentValue(it, PM.PM10))
                    }
                }
                PM.PM25 -> {
                    entity.pm25?.let {
                        valuesToDisplay.add(unitsConverter.getPmEnvironmentValue(it, PM.PM25))
                    }
                }
                PM.PM40 -> {
                    entity.pm4?.let {
                        valuesToDisplay.add(unitsConverter.getPmEnvironmentValue(it, PM.PM40))
                    }
                }
                PM.PM100 -> {
                    entity.pm10?.let {
                        valuesToDisplay.add(unitsConverter.getPmEnvironmentValue(it, PM.PM100))
                    }
                }
                MsnUnit.MsnCount -> {
                    entity.measurementSequenceNumber?.let {
                        valuesToDisplay.add(unitsConverter.getMsnValue(it))
                    }
                }
                else -> {}
            }
        }

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
            defaultDisplayOrder = entity.defaultDisplayOrder,
            displayOrder = displayOrder,
            possibleDisplayOptions = possibleOptionsFiltered,
            valuesToDisplay = valuesToDisplay,
            latestMeasurement = entity.latestId?.let {
                SensorMeasurements(
                    aqi = unitsConverter.getAqiEnviromentValue(AQI.getAQI(
                        pm25 = entity.pm25,
                        co2 = entity.co2)
                    ),
                    temperature = temperature?.let {
                        unitsConverter.getTemperatureEnvironmentValue(it)
                    },
                    pressure = pressure?.let {
                        unitsConverter.getPressureEnvironmentValue(it)
                    },
                    humidity = humidity?.let {
                        unitsConverter.getHumidityEnvironmentValue(it, temperature)
                    },
                    movement = entity.movementCounter?.let {
                        movementConverter.getMovementEnvironmentValue(it)
                    },
                    voltage = unitsConverter.getVoltageEnvironmentValue(entity.voltage),
                    rssi = unitsConverter.getSignalEnvironmentValue(entity.rssi),
                    accelerationX = entity.accelX,
                    accelerationY = entity.accelY,
                    accelerationZ = entity.accelZ,
                    txPower = entity.txPower,
                    pm10 = entity.pm1?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM10) },
                    pm25 = entity.pm25?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM25) },
                    pm40 = entity.pm4?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM40) },
                    pm100 = entity.pm10?.let { unitsConverter.getPmEnvironmentValue(it, PM.PM100) },
                    co2 = entity.co2?.let { unitsConverter.getCo2EnvironmentValue(it) },
                    nox = entity.nox?.let { unitsConverter.getNoxEnvironmentValue(it) },
                    voc = entity.voc?.let { unitsConverter.getVocEnvironmentValue(it) },
                    luminosity = entity.luminosity?.let { unitsConverter.getLuminosityEnvironmentValue(it) },
                    dBaAvg = entity.dBaAvg?.let { unitsConverter.getSoundEnvironmentValue(it, SoundAvg.SoundDba) },
                    dBaPeak = entity.dBaPeak?.let { unitsConverter.getSoundEnvironmentValue(it, SoundPeak.SoundDba) },
                    dataFormat = entity.dataFormat,
                    measurementSequenceNumber = entity.measurementSequenceNumber,
                    connectable = entity.connectable,
                    updatedAt = entity.updateAt,
                )
            }
        )
    }


}