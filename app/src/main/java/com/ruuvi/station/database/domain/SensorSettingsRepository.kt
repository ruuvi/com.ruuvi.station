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

    fun getSensorSettings(): List<SensorSettings> =
        SQLite
            .select()
            .from(SensorSettings::class.java)
            .queryList()

    fun getSensorSettingsOrCreate(sensorId: String): SensorSettings {
        var settings = getSensorSettings(sensorId)
        if (settings == null) {
            settings = SensorSettings(sensorId, Date())
            setKindaRandomBackground(settings)
            settings.insert()
        }
        return settings
    }

    fun updateSensorName(sensorId: String, sensorName: String?) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.name = sensorName
        settings.update()
    }

    fun setSensorTemperatureCalibrationOffset(sensorId: String, temperatureOffset: Double) {
        var settings = getSensorSettingsOrCreate(sensorId)
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

    fun setSensorPressureCalibrationOffset(sensorId: String, offset: Double) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.pressureOffset = offset
        settings.pressureOffsetDate = Date()
        settings.update()
    }

    fun clearPressureCalibration(sensorId: String) {
        var settings = getSensorSettings(sensorId)
        settings?.let {
            it.pressureOffset = null
            it.pressureOffsetDate = null
            it.update()
        }
    }

    fun setSensorHumidityOffset(sensorId: String, offset: Double) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.humidityOffset = offset
        settings.humidityOffsetDate = Date()
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

    fun updateSensorBackground(sensorId: String, userBackground: String?, defaultBackground: Int?, networkBackground: String?) {
        SQLite.update(SensorSettings::class.java)
            .set(
                SensorSettings_Table.userBackground.eq(userBackground),
                SensorSettings_Table.defaultBackground.eq(defaultBackground),
                SensorSettings_Table.networkBackground.eq(networkBackground)
            )
            .where(SensorSettings_Table.id.eq(sensorId))
            .execute()
    }

    fun updateNetworkBackground(sensorId: String, networkBackground: String?) {
        SQLite.update(SensorSettings::class.java)
            .set(
                SensorSettings_Table.networkBackground.eq(networkBackground)
            )
            .where(SensorSettings_Table.id.eq(sensorId))
            .async()
            .execute()
    }

    fun setSensorOwner(sensorId: String, owner: String) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.owner = owner
        settings.update()
    }

    fun setKindaRandomBackground(sensorSettings: SensorSettings){
        val settings = getSensorSettings()
        var background = (Math.random() * 9.0).toInt()
        for (i in 0..99) {
            if (settings.none { it.defaultBackground == background }) break
            background = (Math.random() * 9.0).toInt()
        }
        sensorSettings.defaultBackground = background
    }

    fun updateLastSync(sensorId: String, date: Date?) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.lastSync = date
        settings.update()
    }

    fun clearLastSync(sensorId: String) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.lastSync = null
        settings.networkLastSync = null
        settings.update()
    }

    fun updateNetworkLastSync(sensorId: String, date: Date) {
        var settings = getSensorSettingsOrCreate(sensorId)
        settings.networkLastSync = date
        settings.update()
    }

    fun clearLastSyncGatt() {
        SQLite.update(SensorSettings::class.java)
            .set(SensorSettings_Table.lastSync.eq(null))
            .execute()
    }
}