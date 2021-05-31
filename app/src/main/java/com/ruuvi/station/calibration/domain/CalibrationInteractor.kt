package com.ruuvi.station.calibration.domain

import com.ruuvi.station.calibration.model.CalibrationInfo
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.HumidityUnit

class CalibrationInteractor (
    val tagRepository: TagRepository,
    val sensorSettingsRepository: SensorSettingsRepository,
    val unitsConverter: UnitsConverter,
    private val networkInteractor: RuuviNetworkInteractor
    ) {
    fun getSensorData(sensorId: String): RuuviTagEntity? = tagRepository.getTagById(sensorId)

    fun getTemperatureString(temperature: Double?): String = unitsConverter.getTemperatureString(temperature)

    fun getSensorSettings(sensorId: String): SensorSettings? = sensorSettingsRepository.getSensorSettings(sensorId)

    fun calibrateTemperature(sensorId: String, targetValue: Double) {
        val data = getSensorData(sensorId)
        data?.let {
            val fromTemperature = data.temperature - data.temperatureOffset
            val targetCelsius = unitsConverter.getTemperatureCelsiusValue(targetValue)
            val offset = targetCelsius - fromTemperature
            sensorSettingsRepository.setSensorTemperatureCalibrationOffset(sensorId, offset)
            saveCalibrationToNetwork(sensorId)
        }
    }

    fun getTemperatureUnit(): String = unitsConverter.getTemperatureUnitString()

    fun clearTemperatureCalibration(sensorId: String) {
        sensorSettingsRepository.clearTemperatureCalibration(sensorId)
        saveCalibrationToNetwork(sensorId)
    }

    private fun isTemperatureCalibrated(settings: SensorSettings?): Boolean = settings?.temperatureOffset ?: 0.0 != 0.0

    private fun isPressureCalibrated(settings: SensorSettings?): Boolean = settings?.pressureOffset ?: 0.0 != 0.0

    private fun isHumidityCalibrated(settings: SensorSettings?): Boolean = settings?.humidityOffset ?: 0.0 != 0.0

    fun getTemperatureCalibrationInfo(sensorId: String): CalibrationInfo? {
        val data = getSensorData(sensorId)
        val sensorSettings = getSensorSettings(sensorId)
        val currentTemperatureOffset = getSensorSettings(sensorId)?.temperatureOffset ?: 0.0
        val isTemperatureCalibrated = isTemperatureCalibrated(sensorSettings)
        data?.let {
            val temperatureRaw = data.temperature - data.temperatureOffset
            val temperatureCalibrated = temperatureRaw + currentTemperatureOffset
            return CalibrationInfo(
                sensorId,
                it.updateAt,
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
        val data = getSensorData(sensorId)
        val sensorSettings = getSensorSettings(sensorId)
        val currentPressureOffset = getSensorSettings(sensorId)?.pressureOffset ?: 0.0
        val isPressureCalibrated = isPressureCalibrated(sensorSettings)
        data?.let {
            val rawValue = (data.pressure ?: 0.0) - data.pressureOffset
            val calibratedValue = rawValue + currentPressureOffset
            return CalibrationInfo(
                sensorId,
                it.updateAt,
                rawValue,
                calibratedValue,
                isPressureCalibrated,
                currentPressureOffset,
                currentOffsetString = unitsConverter.getPressureString(currentPressureOffset)
            )
        }
        return null
    }

    fun getHumidityCalibrationInfo(sensorId: String): CalibrationInfo? {
        val data = getSensorData(sensorId)
        val sensorSettings = getSensorSettings(sensorId)
        val currentHumidityOffset = getSensorSettings(sensorId)?.humidityOffset ?: 0.0
        val isHumidityCalibrated = isHumidityCalibrated(sensorSettings)
        data?.let {
            val rawValue = (data.humidity ?: 0.0) - data.humidityOffset
            val calibratedValue = rawValue + currentHumidityOffset
            return CalibrationInfo(
                sensorId,
                it.updateAt,
                rawValue,
                calibratedValue,
                isHumidityCalibrated,
                currentHumidityOffset,
                currentOffsetString = unitsConverter.getHumidityString(currentHumidityOffset, 0.0, HumidityUnit.PERCENT)
            )
        }
        return null
    }

    fun getPressureUnit(): String = unitsConverter.getPressureUnitString()

    fun getPressureString(value: Double): String = unitsConverter.getPressureString(value)

    fun clearPressureCalibration(sensorId: String) {
        sensorSettingsRepository.clearPressureCalibration(sensorId)
        saveCalibrationToNetwork(sensorId)
    }

    fun calibratePressure(sensorId: String, targetValue: Double) {
        val data = getSensorData(sensorId)
        data?.let {
            val fromPressure = (data.pressure ?: 0.0) - data.pressureOffset
            val targetPascal = unitsConverter.getPressurePascalValue(targetValue)
            val offset = targetPascal - fromPressure
            sensorSettingsRepository.setSensorPressureCalibrationOffset(sensorId, offset)
            saveCalibrationToNetwork(sensorId)
        }
    }

    fun getHumidityUnit(): String = unitsConverter.getHumidityUnitString(HumidityUnit.PERCENT)

    fun getHumidityString(value: Double): String = unitsConverter.getHumidityString(value, 0.0, HumidityUnit.PERCENT)

    fun clearHumidityCalibration(sensorId: String) {
        sensorSettingsRepository.clearHumidityCalibration(sensorId)
        saveCalibrationToNetwork(sensorId)
    }

    fun calibrateHumidity(sensorId: String, targetValue: Double) {
        val data = getSensorData(sensorId)
        data?.let {
            val fromHumidity = (data.humidity ?: 0.0) - data.humidityOffset
            val offset = targetValue - fromHumidity
            sensorSettingsRepository.setSensorHumidityOffset(sensorId, offset)
            saveCalibrationToNetwork(sensorId)
        }
    }

    fun saveCalibrationToNetwork(sensorId: String) {
        networkInteractor.updateSensorCalibration(sensorId)
    }
}