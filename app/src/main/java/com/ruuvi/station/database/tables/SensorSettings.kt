package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.*
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.calibration.domain.CalibrationInteractor
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.network.data.response.SensorDataResponse
import java.util.*

@Table (
    database = LocalDatabase::class,
    useBooleanGetterSetters = false
)
data class SensorSettings(
    @Column
    @PrimaryKey
    var id: String = "",
    @Column
    var createDate: Date? = null,
    @Column
    var name: String? = null,
    @Column
    var defaultBackground: Int = 0,
    @Column
    var userBackground: String? = null,
    @Column
    var networkBackground: String? = null,
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
    var pressureOffsetDate: Date? = null,
    @Column
    var owner: String? = null,
    @Column
    var lastSync: Date? = null,
    @Column
    var networkLastSync: Date? = null,
    @Column
    var networkSensor: Boolean = false
): BaseModel() {
    val displayName get() = if (name.isNullOrEmpty()) id else name.toString()

    fun calibrateSensor(sensor: RuuviTagEntity) {
        sensor.temperature += (temperatureOffset ?: 0.0)
        sensor.temperatureOffset = temperatureOffset ?: 0.0

        sensor.pressure?.let { pressure ->
            sensor.pressure = pressure + (pressureOffset ?: 0.0)
        }
        sensor.pressureOffset = pressureOffset ?: 0.0

        sensor.humidity?.let { humidity ->
            sensor.humidity = humidity + (humidityOffset ?: 0.0)
        }
        sensor.humidityOffset = humidityOffset ?: 0.0
    }

    fun calibrateSensor(sensorReading: TagSensorReading) {
        sensorReading.temperature += (temperatureOffset ?: 0.0)
        sensorReading.temperatureOffset = temperatureOffset ?: 0.0

        sensorReading.pressure?.let { pressure ->
            sensorReading.pressure = pressure + (pressureOffset ?: 0.0)
        }
        sensorReading.pressureOffset = pressureOffset ?: 0.0

        sensorReading.humidity?.let { humidity ->
            sensorReading.humidity = humidity + (humidityOffset ?: 0.0)
        }
        sensorReading.humidityOffset = humidityOffset ?: 0.0
    }

    fun updateFromNetwork(sensor: SensorDataResponse, calibrationInteractor: CalibrationInteractor) {
        name = sensor.name
        owner = sensor.owner
        val recalibrateHistory = humidityOffset != sensor.offsetHumidity || pressureOffset != sensor.offsetPressure || temperatureOffset != sensor.offsetTemperature
        humidityOffset = sensor.offsetHumidity
        pressureOffset = sensor.offsetPressure
        temperatureOffset = sensor.offsetTemperature
        networkSensor = true
        update()
        if (recalibrateHistory) calibrationInteractor.recalibrateHistory(this)
    }
}