package com.ruuvi.station.database.domain

import com.raizlabs.android.dbflow.sql.language.SQLite
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.SensorSettings_Table
import timber.log.Timber
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
        val settings = getSensorSettings(sensorId)
        settings?.let {
            it.pressureOffset = null
            it.pressureOffsetDate = null
            it.update()
        }
    }

    fun setSensorHumidityOffset(sensorId: String, offset: Double) {
        val settings = getSensorSettingsOrCreate(sensorId)
        settings.humidityOffset = offset
        settings.humidityOffsetDate = Date()
        settings.update()
    }

    fun clearHumidityCalibration(sensorId: String) {
        val settings = getSensorSettings(sensorId)
        settings?.let {
            it.humidityOffset = null
            it.humidityOffsetDate = null
            it.update()
        }
    }

    fun updateSensorBackground(sensorId: String, userBackground: String?, defaultBackground: Int?, networkBackground: String?) {
        Timber.d("updateSensorBackground $sensorId $networkBackground")
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
        Timber.d("updateNetworkBackground $sensorId $networkBackground")
        SQLite.update(SensorSettings::class.java)
            .set(
                SensorSettings_Table.networkBackground.eq(networkBackground)
            )
            .where(SensorSettings_Table.id.eq(sensorId))
            .async()
            .execute()
    }

    fun setSensorOwner(sensorId: String, owner: String?, isNetworkSensor: Boolean?) {
        val settings = getSensorSettingsOrCreate(sensorId)
        settings.owner = owner
        settings.networkSensor = isNetworkSensor ?: settings.networkSensor
        if (owner == null) {
            settings.subscriptionName = null
        }
        settings.update()
    }

    fun updateLastSync(sensorId: String, date: Date?) {
        val settings = getSensorSettingsOrCreate(sensorId)
        settings.lastSync = date
        settings.update()
    }

    fun clearLastSync(sensorId: String) {
        val settings = getSensorSettingsOrCreate(sensorId)
        settings.lastSync = null
        settings.networkHistoryLastSync = null
        settings.update()
    }

    fun updateNetworkLastSync(sensorId: String, date: Date) {
        val settings = getSensorSettingsOrCreate(sensorId)
        settings.networkLastSync = date
        settings.update()
    }

    fun updateNetworkHistoryLastSync(sensorId: String, date: Date) {
        val settings = getSensorSettingsOrCreate(sensorId)
        settings.networkHistoryLastSync = date
        settings.update()
    }

    fun clearLastSyncGatt() {
        SQLite.update(SensorSettings::class.java)
            .set(SensorSettings_Table.lastSync.eq(null))
            .execute()
    }

    fun setSensorFirmware(sensorId: String, firmware: String?) {
        getSensorSettings(sensorId)?.let {
            it.firmware = firmware
            it.update()
        }
    }

    fun updateUseDefaultSensorOrder(sensorId: String, enabled: Boolean) {
        getSensorSettings(sensorId)?.let { settings ->
            settings.defaultDisplayOrder = enabled
            settings.update()
        }
    }

    fun newDisplayOrder(sensorId: String, displayOrder: String) {
        getSensorSettings(sensorId)?.let { settings ->
            settings.displayOrder = displayOrder
            settings.update()
        }
    }
}