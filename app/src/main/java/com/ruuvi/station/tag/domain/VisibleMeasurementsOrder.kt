package com.ruuvi.station.tag.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.tag.domain.RuuviTag.Companion.dataFormatIsAir
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.UnitType.Acceleration
import com.ruuvi.station.units.model.UnitType.AirQuality
import com.ruuvi.station.units.model.UnitType.BatteryVoltageUnit
import com.ruuvi.station.units.model.UnitType.CO2
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import com.ruuvi.station.units.model.UnitType.Luminosity
import com.ruuvi.station.units.model.UnitType.MovementUnit
import com.ruuvi.station.units.model.UnitType.NOX
import com.ruuvi.station.units.model.UnitType.PM
import com.ruuvi.station.units.model.UnitType.PressureUnit
import com.ruuvi.station.units.model.UnitType.SignalStrengthUnit
import com.ruuvi.station.units.model.UnitType.SoundAvg
import com.ruuvi.station.units.model.UnitType.SoundPeak
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import com.ruuvi.station.units.model.UnitType.VOC
import com.ruuvi.station.util.extensions.loadList
import timber.log.Timber

class VisibleMeasurementsOrderInteractor(
    private val preferencesRepository: PreferencesRepository,
    private val sensorSettingsRepository: SensorSettingsRepository
) {
    fun getDefaultDisplayOrder(
        ruuviTag: RuuviTag
    ): List<UnitType> {
        return getDefaultDisplayOrder(
            isAir = ruuviTag.isAir(),
            humidityExist = ruuviTag.latestMeasurement?.humidity != null,
            pressureExist = ruuviTag.latestMeasurement?.pressure != null,
            lightExist = ruuviTag.latestMeasurement?.luminosity != null
        )
    }

    fun getDefaultDisplayOrder(entity: FavouriteSensorQuery): List<UnitType> {
        return getDefaultDisplayOrder(
            isAir = dataFormatIsAir(entity.dataFormat),
            humidityExist = entity.humidity != null,
            pressureExist = entity.pressure != null,
            lightExist = entity.luminosity != null
        )
    }

    fun getDefaultDisplayOrder(
        isAir: Boolean,
        humidityExist: Boolean,
        pressureExist: Boolean,
        lightExist: Boolean
    ): List<UnitType> {
        val displayOrder = mutableListOf<UnitType>()

        if (isAir) {
            displayOrder.add(AirQuality.AqiIndex)
            displayOrder.add(CO2.Ppm)
            displayOrder.add(PM.PM25)
            displayOrder.add(VOC.VocIndex)
            displayOrder.add(NOX.NoxIndex)
            displayOrder.add(preferencesRepository.getTemperatureUnit())
            if (humidityExist) {
                displayOrder.add(preferencesRepository.getHumidityUnit())
            }
            if (pressureExist) {
                displayOrder.add(preferencesRepository.getPressureUnit())
            }
            if (lightExist) {
                displayOrder.add(Luminosity.Lux)
            }
        } else {
            displayOrder.add(preferencesRepository.getTemperatureUnit())
            if (humidityExist) {
                displayOrder.add(preferencesRepository.getHumidityUnit())
            }
            if (pressureExist) {
                displayOrder.add(preferencesRepository.getPressureUnit())
            }
            displayOrder.add(MovementUnit.MovementsCount)
        }
        return displayOrder
    }

    fun getUserDefinedOrder(
        displayOrder: String?,
        defaultOrder: List<UnitType>
    ): List<UnitType> {
        val userOrder =  UnitType.getListOfUnits(displayOrder?.loadList() ?: emptyList())
        return userOrder.ifEmpty { defaultOrder }
    }


    fun getPossibleDisplayOptions(
        entity: FavouriteSensorQuery
    ): List<UnitType> {
        return getPossibleDisplayOptions(
            isAir = dataFormatIsAir(entity.dataFormat),
            humidityExist = entity.humidity != null,
            pressureExist = entity.pressure != null,
            luminosityExist = entity.luminosity != null,
            soundExist = entity.dBaAvg != null,
        )
    }

    fun getPossibleDisplayOptions(
        entity: RuuviTagEntity
    ): List<UnitType> {
        return getPossibleDisplayOptions(
            isAir = dataFormatIsAir(entity.dataFormat),
            humidityExist = entity.humidity != null,
            pressureExist = entity.pressure != null,
            luminosityExist = entity.luminosity != null,
            soundExist = entity.dBaAvg != null,
        )
    }

    fun getPossibleDisplayOptions(
        isAir: Boolean,
        humidityExist: Boolean,
        pressureExist: Boolean,
        luminosityExist: Boolean,
        soundExist: Boolean
    ): List<UnitType> {
        val displayOptions = mutableListOf<UnitType>()

        if (isAir) {
            displayOptions.add(AirQuality.AqiIndex)
            displayOptions.add(TemperatureUnit.Celsius)
            displayOptions.add(TemperatureUnit.Fahrenheit)
            displayOptions.add(TemperatureUnit.Kelvin)
            if (humidityExist) {
                displayOptions.add(HumidityUnit.Relative)
                displayOptions.add(HumidityUnit.Absolute)
                displayOptions.add(HumidityUnit.DewPoint)
            }
            if (pressureExist) {
                displayOptions.add(PressureUnit.Pascal)
                displayOptions.add(PressureUnit.HectoPascal)
                displayOptions.add(PressureUnit.MmHg)
                displayOptions.add(PressureUnit.InchHg)
            }
            if (luminosityExist) {
                displayOptions.add(Luminosity.Lux)
            }
            if (soundExist) {
                displayOptions.add(SoundAvg.SoundDba)
                displayOptions.add(SoundPeak.SoundDba)
            }
            displayOptions.add(CO2.Ppm)
            displayOptions.add(VOC.VocIndex)
            displayOptions.add(NOX.NoxIndex)
            displayOptions.add(PM.PM10)
            displayOptions.add(PM.PM25)
            displayOptions.add(PM.PM40)
            displayOptions.add(PM.PM100)
            displayOptions.add(UnitType.MsnUnit.MsnCount)
        } else {
            displayOptions.add(TemperatureUnit.Celsius)
            displayOptions.add(TemperatureUnit.Fahrenheit)
            displayOptions.add(TemperatureUnit.Kelvin)
            if (humidityExist) {
                displayOptions.add(HumidityUnit.Relative)
                displayOptions.add(HumidityUnit.Absolute)
                displayOptions.add(HumidityUnit.DewPoint)
            }
            if (pressureExist) {
                displayOptions.add(PressureUnit.Pascal)
                displayOptions.add(PressureUnit.HectoPascal)
                displayOptions.add(PressureUnit.MmHg)
                displayOptions.add(PressureUnit.InchHg)
            }
            displayOptions.add(MovementUnit.MovementsCount)
            displayOptions.add(BatteryVoltageUnit.Volt)
            displayOptions.add(SignalStrengthUnit.SignalDbm)
            displayOptions.add(Acceleration.GForceX)
            displayOptions.add(Acceleration.GForceY)
            displayOptions.add(Acceleration.GForceZ)
            displayOptions.add(UnitType.MsnUnit.MsnCount)
        }
        Timber.d("getPossibleDisplayOptions = $displayOptions")

        return displayOptions
    }

}