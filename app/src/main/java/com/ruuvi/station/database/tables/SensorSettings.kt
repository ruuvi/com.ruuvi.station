package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.database.LocalDatabase
import timber.log.Timber
import java.util.*

@Table (name = "SensorSettings", database = LocalDatabase::class)
data class SensorSettings(
    @Column
    @PrimaryKey
    var id: String = "",
    @Column
    var humidityOffset: Double? = null,
    @Column
    var humidityOffsetDate: Date? = null,
    @Column
    var temperatureOffset: Double? = null,
    @Column
    var temperatureOffsetDate: Date? = null,
    @Column
    var pressureOffset: Double? = null,
    @Column
    var pressureOffsetDate: Date? = null
): BaseModel() {
    fun calibrateSensor(sensor: RuuviTagEntity) {
        Timber.d("calibration ${sensor.id} before ${sensor.temperature}")
        sensor.temperature += (temperatureOffset ?: 0.0)
        Timber.d("calibration ${sensor.id} after ${sensor.temperature}")
    }
}