package com.ruuvi.station.calibration.domain

import com.ruuvi.station.database.SensorSettingsRepository
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.units.domain.UnitsConverter

class CalibrationInteractor (
    val tagRepository: TagRepository,
    val sensorSettingsRepository: SensorSettingsRepository,
    val unitsConverter: UnitsConverter
) {
    fun getSensorData(sensorId: String): RuuviTagEntity? = tagRepository.getTagById(sensorId)

    fun getTemperatureString(temperature: Double?): String = unitsConverter.getTemperatureString(temperature)

    fun getTemperatureStringWithoutUnit(temperature: Double?): String = unitsConverter.getTemperatureStringWithoutUnit(temperature)

    fun getSensorSettings(sensorId: String): SensorSettings? = sensorSettingsRepository.getSensorSettings(sensorId)

    fun getOriginalTemperature(sensorId: String, temperature: Double): Double {
        val settings = getSensorSettings(sensorId)
        return temperature - (settings?.temperatureOffset ?: 0.0)
    }

    fun getTemperatureOffset(sensorId: String): String =
        unitsConverter.getTemperatureOffsetString(getSensorSettings(sensorId)?.temperatureOffset ?: 0.0)

    fun calibrateTemperature(sensorId: String, fromTemperature: Double, targetValue: Double) {
        val targetCelsius = unitsConverter.getTemperatureCelsiusValue(targetValue)
        val offset = targetCelsius - fromTemperature
        sensorSettingsRepository.setSensorTemperatureCalibrationOffset(sensorId, offset)
    }

    fun getTemperatureUnit(): String = unitsConverter.getTemperatureUnitString()

    fun clearTemperatureCalibration(sensorId: String) {
        sensorSettingsRepository.clearTemperatureCalibration(sensorId)
    }
}