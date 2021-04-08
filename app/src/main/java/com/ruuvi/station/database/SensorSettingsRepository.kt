package com.ruuvi.station.database

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
//        SQLite
//            .update(SensorSettings::class.java)
//            .set(
//                SensorSettings_Table.temperatureOffset.eq(temperatureOffset),
//                SensorSettings_Table.temperatureOffsetDate.eq(Date())
//            )
//            .where(SensorSettings_Table.id.eq(sensorId))
//            .async()
//            .execute()
    }

    fun clearTemperatureCalibration(sensorId: String) {
        var settings = getSensorSettings(sensorId)
        settings?.let {
            it.temperatureOffset = null
            it.temperatureOffsetDate = null
            it.update()
        }

    }
}