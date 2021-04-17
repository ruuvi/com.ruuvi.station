package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.SensorSettings_Table
import java.util.*

class SensorSettingsRepository {
    fun getSensorSettings(sensorId: String): SensorSettings? =
        SQLite
            .select()
            .from(SensorSettings::class.java)
            .where(SensorSettings_Table.id.eq(sensorId))
            .querySingle()

    fun setSensorTemperatureCalibrationOffset(sensorId: String, temperatureOffset: Double) {
        var settings = getSensorSettings(sensorId)
        if (settings == null) {
            settings = SensorSettings(sensorId)
            settings.insert()
        }
        settings.temperatureOffset = temperatureOffset
        settings.temperatureOffsetDate = Date()
        settings.update()
    }

    fun clearTemperatureCalibration(sensorId: String) {
        var settings = getSensorSettings(sensorId)
        settings?.let {
            it.temperatureOffset = null
            it.temperatureOffsetDate = null
            it.update()
        }
    }

    fun clearPressureCalibration(sensorId: String) {
        var settings = getSensorSettings(sensorId)
        settings?.let {
            it.pressureOffset = null
            it.pressureOffsetDate = null
            it.update()
        }
    }

    fun setSensorPressureCalibrationOffset(sensorId: String, offset: Double) {
        var settings = getSensorSettings(sensorId)
        if (settings == null) {
            settings = SensorSettings(sensorId)
            settings.insert()
        }
        settings.pressureOffset = offset
        settings.pressureOffsetDate = Date()
        settings.update()
    }

    fun clearHumidityCalibration(sensorId: String) {
        var settings = getSensorSettings(sensorId)
        settings?.let {
            it.humidityOffset = null
            it.humidityOffsetDate = null
            it.update()
        }
    }

    fun setSensorHumidityOffset(sensorId: String, offset: Double) {
        var settings = getSensorSettings(sensorId)
        if (settings == null) {
            settings = SensorSettings(sensorId)
            settings.insert()
        }
        settings.humidityOffset = offset
        settings.humidityOffsetDate = Date()
        settings.update()
    }
}