package com.ruuvi.station.calibration.domain

import com.ruuvi.station.calibration.model.CalibrationInfo
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

    fun getSensorSettings(sensorId: String): SensorSettings? = sensorSettingsRepository.getSensorSettings(sensorId)

    fun calibrateTemperature(sensorId: String, targetValue: Double) {
        val data = getSensorData(sensorId)
        data?.let {
            val fromTemperature = data.temperature - (data.temperatureOffset ?: 0.0)
            val targetCelsius = unitsConverter.getTemperatureCelsiusValue(targetValue)
            val offset = targetCelsius - fromTemperature
            sensorSettingsRepository.setSensorTemperatureCalibrationOffset(sensorId, offset)
        }
    }

    fun getTemperatureUnit(): String = unitsConverter.getTemperatureUnitString()

    fun clearTemperatureCalibration(sensorId: String) {
        sensorSettingsRepository.clearTemperatureCalibration(sensorId)
    }

    private fun isTemperatureCalibrated(settings: SensorSettings?): Boolean = settings?.temperatureOffset ?: 0.0 != 0.0

    fun getCalibrationInfo(sensorId: String): CalibrationInfo? {
        val data = getSensorData(sensorId)
        val sensorSettings = getSensorSettings(sensorId)
        val currentTemperatureOffset = getSensorSettings(sensorId)?.temperatureOffset ?: 0.0
        val isTemperatureCalibrated = isTemperatureCalibrated(sensorSettings)
        data?.let {
            val temperatureRaw = data.temperature - (data.temperatureOffset ?: 0.0)
            val temperatureCalibrated = temperatureRaw + currentTemperatureOffset
            return CalibrationInfo(
                sensorId,
                it.updateAt,
                temperatureRaw,
                temperatureCalibrated,
                isTemperatureCalibrated,
                currentTemperatureOffset,
                currentTemperatureOffsetString = unitsConverter.getTemperatureOffsetString(currentTemperatureOffset)
            )
        }
        return null
    }
}