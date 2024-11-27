package com.ruuvi.station.dataforwarding.data

import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import java.util.*

data class SensorInfo(
    var id: String? = null,
    var rssi: Int? = null,
    var name: String? = null,
    var temperature: Double? = null,
    var temperatureOffset: Double = 0.0,
    var humidity: Double? = null,
    var humidityOffset: Double = 0.0,
    var pressure: Double? = null,
    var pressureOffset: Double = 0.0,
    var favorite: Boolean = false,
    var accelX: Double? = null,
    var accelY: Double? = null,
    var accelZ: Double? = null,
    var voltage: Double? = null,
    var updateAt: Date? = null,
    var dataFormat: Int? = null,
    var txPower: Double? = null,
    var movementCounter: Int? = null,
    var measurementSequenceNumber: Int? = null,
    var createDate: Date? = null,
    var connectable: Boolean = false
) {
    constructor(tagEntity: RuuviTagEntity, sensorSettings: SensorSettings) :
            this(
                id = tagEntity.id,
                rssi = tagEntity.rssi,
                name = sensorSettings.displayName,
                temperature = tagEntity.temperature?.let { it + (sensorSettings.temperatureOffset ?: 0.0)},
                temperatureOffset = sensorSettings.temperatureOffset ?: 0.0,
                humidity = tagEntity.humidity?.let {it + (sensorSettings.humidityOffset ?: 0.0)},
                humidityOffset = sensorSettings.humidityOffset ?: 0.0,
                pressure = tagEntity.pressure?.let {it + (sensorSettings.pressureOffset ?: 0.0)},
                pressureOffset = sensorSettings.pressureOffset ?: 0.0,
                favorite = tagEntity.favorite,
                accelX = tagEntity.accelX,
                accelY = tagEntity.accelY,
                accelZ = tagEntity.accelZ,
                voltage = tagEntity.voltage,
                updateAt = tagEntity.updateAt,
                dataFormat = tagEntity.dataFormat,
                txPower = tagEntity.txPower,
                movementCounter = tagEntity.movementCounter,
                measurementSequenceNumber = tagEntity.measurementSequenceNumber,
                createDate = sensorSettings.createDate,
                connectable = tagEntity.connectable
            )

    companion object {
        fun fromGattLogReading(logReading: LogReading, sensorSettings: SensorSettings): SensorInfo {
            return SensorInfo(
                id = logReading.id,
                name = sensorSettings.displayName,
                temperature = logReading.temperature + (sensorSettings.temperatureOffset ?: 0.0),
                temperatureOffset = sensorSettings.temperatureOffset ?: 0.0,
                humidity = logReading.humidity?.let {it + (sensorSettings.humidityOffset ?: 0.0)},
                humidityOffset = sensorSettings.humidityOffset ?: 0.0,
                pressure = logReading.pressure?.let {it + (sensorSettings.pressureOffset ?: 0.0)},
                pressureOffset = sensorSettings.pressureOffset ?: 0.0,
                favorite = true,
                updateAt = logReading.date,
                createDate = sensorSettings.createDate,
                connectable = true
            )
        }
    }
}