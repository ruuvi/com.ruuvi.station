package com.ruuvi.station.tag.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.FavouriteSensorQuery
import com.ruuvi.station.tag.domain.RuuviTag.Companion.dataFormatIsAir
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.UnitType.AirQuality
import com.ruuvi.station.units.model.UnitType.CO2
import com.ruuvi.station.units.model.UnitType.Luminosity
import com.ruuvi.station.units.model.UnitType.MovementUnit
import com.ruuvi.station.units.model.UnitType.NOX
import com.ruuvi.station.units.model.UnitType.PM
import com.ruuvi.station.units.model.UnitType.VOC
import com.ruuvi.station.util.extensions.loadList

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
            pressureExist = ruuviTag.latestMeasurement?.pressure != null
        )
    }

    fun getDefaultDisplayOrder(entity: FavouriteSensorQuery): List<UnitType> {
        return getDefaultDisplayOrder(
            isAir = dataFormatIsAir(entity.dataFormat),
            humidityExist = entity.humidity != null,
            pressureExist = entity.pressure != null
        )
    }

    fun getDefaultDisplayOrder(
        isAir: Boolean,
        humidityExist: Boolean,
        pressureExist: Boolean
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
            displayOrder.add(Luminosity.Lux)
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

    fun getUserDefinedOrder(displayOrder: String?): List<UnitType> {
        return UnitType.getListOfUnits(displayOrder?.loadList() ?: emptyList())
    }

}