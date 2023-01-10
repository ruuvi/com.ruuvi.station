package com.ruuvi.station.calibration.domain

import com.ruuvi.station.calibration.model.CalibrationInfo
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.HumidityUnit
import com.ruuvi.station.util.extensions.round

class CalibrationInteractor (
    val tagRepository: TagRepository,
    val sensorSettingsRepository: SensorSettingsRepository,
    val unitsConverter: UnitsConverter,
    val sensorHistoryRepository: SensorHistoryRepository,
    private val networkInteractor: RuuviNetworkInteractor
    ) {
    private fun getSensorEntity(sensorId: String): RuuviTagEntity? = tagRepository.getTagById(sensorId)

    fun getTemperatureString(temperature: Double?): String = unitsConverter.getTemperatureString(temperature, Accuracy.Accuracy2)

    fun getSensorSettings(sensorId: String): SensorSettings? = sensorSettingsRepository.getSensorSettings(sensorId)

    fun calibrateTemperature(sensorId: String, targetValue: Double) {
        getSensorEntity(sensorId)?.let { entity ->
            val fromTemperature = entity.temperature - entity.temperatureOffset
            val targetCelsius = unitsConverter.getTemperatureCelsiusValue(targetValue).round(2)
            val offset = targetCelsius - fromTemperature
            sensorSettingsRepository.setSensorTemperatureCalibrationOffset(sensorId, offset)
            saveCalibrationToNetwork(sensorId)
            recalibrateHistory(sensorId)
        }
    }

    fun getTemperatureUnit(): String = unitsConverter.getTemperatureUnitString()

    fun clearTemperatureCalibration(sensorId: String) {
        sensorSettingsRepository.clearTemperatureCalibration(sensorId)
        saveCalibrationToNetwork(sensorId)
        recalibrateHistory(sensorId)
    }

    private fun isTemperatureCalibrated(settings: SensorSettings?): Boolean = settings?.temperatureOffset ?: 0.0 != 0.0

    private fun isPressureCalibrated(settings: SensorSettings?): Boolean = settings?.pressureOffset ?: 0.0 != 0.0

    private fun isHumidityCalibrated(settings: SensorSettings?): Boolean = settings?.humidityOffset ?: 0.0 != 0.0

    fun getTemperatureCalibrationInfo(sensorId: String): CalibrationInfo? {
        val sensorSettings = getSensorSettings(sensorId)
        val currentTemperatureOffset = getSensorSettings(sensorId)?.temperatureOffset ?: 0.0
        val isTemperatureCalibrated = isTemperatureCalibrated(sensorSettings)
        getSensorEntity(sensorId)?.let { entity ->
            val temperatureRaw = entity.temperature - entity.temperatureOffset
            val temperatureCalibrated = temperatureRaw + currentTemperatureOffset
            return CalibrationInfo(
                sensorId,
                entity.updateAt,
                temperatureRaw,
                temperatureCalibrated,
                isTemperatureCalibrated,
                currentTemperatureOffset,
                currentOffsetString = unitsConverter.getTemperatureOffsetString(currentTemperatureOffset)
            )
        }
        return null
    }

    fun getPressureCalibrationInfo(sensorId: String): CalibrationInfo? {
        val sensorSettings = getSensorSettings(sensorId)
        val currentPressureOffset = getSensorSettings(sensorId)?.pressureOffset ?: 0.0
        val isPressureCalibrated = isPressureCalibrated(sensorSettings)
        getSensorEntity(sensorId)?.let { entity ->
            val rawValue = (entity.pressure ?: 0.0) - entity.pressureOffset
            val calibratedValue = rawValue + currentPressureOffset
            return CalibrationInfo(
                sensorId,
                entity.updateAt,
                rawValue,
                calibratedValue,
                isPressureCalibrated,
                currentPressureOffset,
                currentOffsetString = unitsConverter.getPressureString(currentPressureOffset, Accuracy.Accuracy2)
            )
        }
        return null
    }

    fun getHumidityCalibrationInfo(sensorId: String): CalibrationInfo? {
        val sensorSettings = getSensorSettings(sensorId)
        val currentHumidityOffset = getSensorSettings(sensorId)?.humidityOffset ?: 0.0
        val isHumidityCalibrated = isHumidityCalibrated(sensorSettings)
        getSensorEntity(sensorId)?.let { entity ->
            val rawValue = (entity.humidity ?: 0.0) - entity.humidityOffset
            val calibratedValue = rawValue + currentHumidityOffset
            return CalibrationInfo(
                sensorId,
                entity.updateAt,
                rawValue,
                calibratedValue,
                isHumidityCalibrated,
                currentHumidityOffset,
                currentOffsetString = unitsConverter.getHumidityString(currentHumidityOffset, 0.0, HumidityUnit.PERCENT, Accuracy.Accuracy2)
            )
        }
        return null
    }

    fun getPressureUnit(): String = unitsConverter.getPressureUnitString()

    fun getPressureString(value: Double): String = unitsConverter.getPressureString(value, Accuracy.Accuracy2)

    fun clearPressureCalibration(sensorId: String) {
        sensorSettingsRepository.clearPressureCalibration(sensorId)
        saveCalibrationToNetwork(sensorId)
        recalibrateHistory(sensorId)
    }

    fun calibratePressure(sensorId: String, targetValue: Double) {
        getSensorEntity(sensorId)?.let { data ->
            val fromPressure = (data.pressure ?: 0.0) - data.pressureOffset
            val targetPascal = unitsConverter.getPressurePascalValue(targetValue)
            val offset = targetPascal - fromPressure
            sensorSettingsRepository.setSensorPressureCalibrationOffset(sensorId, offset)
            saveCalibrationToNetwork(sensorId)
            recalibrateHistory(sensorId)
        }
    }

    fun getHumidityUnit(): String = unitsConverter.getHumidityUnitString(HumidityUnit.PERCENT)

    fun getHumidityString(value: Double): String = unitsConverter.getHumidityString(value, 0.0, HumidityUnit.PERCENT, Accuracy.Accuracy2)

    fun clearHumidityCalibration(sensorId: String) {
        sensorSettingsRepository.clearHumidityCalibration(sensorId)
        saveCalibrationToNetwork(sensorId)
        recalibrateHistory(sensorId)
    }

    fun calibrateHumidity(sensorId: String, targetValue: Double) {
        getSensorEntity(sensorId)?.let { data ->
            val fromHumidity = (data.humidity ?: 0.0) - data.humidityOffset
            val offset = targetValue - fromHumidity
            sensorSettingsRepository.setSensorHumidityOffset(sensorId, offset)
            saveCalibrationToNetwork(sensorId)
            recalibrateHistory(sensorId)
        }
    }

    fun saveCalibrationToNetwork(sensorId: String) {
        networkInteractor.updateSensorCalibration(sensorId)
    }

    private fun recalibrateHistory(sensorId: String) {
        sensorSettingsRepository.getSensorSettings(sensorId)?.let { sensorSettings ->
            recalibrateHistory(sensorSettings)

            getSensorEntity(sensorId)?.let { entity ->
                sensorSettings.calibrateSensor(entity)
                tagRepository.updateTag(entity)
            }
        }
    }

    fun recalibrateHistory(sensorSettings: SensorSettings) {
        sensorHistoryRepository.recalibrate(sensorSettings)
    }
}