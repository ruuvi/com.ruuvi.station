package com.ruuvi.station.dataforwarding.data

import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.database.tables.TagSensorReading
import java.util.*

data class SensorInfo(
    var id: String? = null,
    var rssi: Int? = null,
    var name: String? = null,
    var temperature: Double = 0.0,
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
                temperature = tagEntity.temperature,
                temperatureOffset = tagEntity.temperatureOffset,
                humidity = tagEntity.humidity,
                humidityOffset = tagEntity.humidityOffset,
                pressure = tagEntity.pressure,
                pressureOffset = tagEntity.pressureOffset,
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
            val reading = TagSensorReading()
            reading.ruuviTagId = logReading.id
            reading.temperature = logReading.temperature
            reading.humidity = logReading.humidity
            reading.pressure = logReading.pressure
            reading.createdAt = logReading.date

            return SensorInfo(
                id = reading.ruuviTagId,
                name = sensorSettings.displayName,
                temperature = reading.temperature,
                temperatureOffset = reading.temperatureOffset,
                humidity = reading.humidity,
                humidityOffset = reading.humidityOffset,
                pressure = reading.pressure,
                pressureOffset = reading.pressureOffset,
                favorite = true,
                updateAt = reading.createdAt,
                createDate = sensorSettings.createDate,
                connectable = true
            )
        }
    }
}