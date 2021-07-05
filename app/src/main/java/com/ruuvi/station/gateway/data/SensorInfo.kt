package com.ruuvi.station.gateway.data

import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import java.util.*

data class SensorInfo(
    var id: String? = null,
    var rssi: Int = 0,
    var name: String? = null,
    var temperature: Double = 0.0,
    var temperatureOffset: Double = 0.0,
    var humidity: Double? = null,
    var humidityOffset: Double = 0.0,
    var pressure: Double? = null,
    var pressureOffset: Double = 0.0,
    var favorite: Boolean = false,
    var accelX: Double = 0.0,
    var accelY: Double = 0.0,
    var accelZ: Double = 0.0,
    var voltage: Double = 0.0,
    var updateAt: Date? = null,
    var dataFormat: Int = 0,
    var txPower: Double = 0.0,
    var movementCounter: Int = 0,
    var measurementSequenceNumber: Int = 0,
    var createDate: Date? = null,
    var connectable: Boolean = false
) {
    companion object {
        fun createEvent(tagEntity: RuuviTagEntity, sensorSettings: SensorSettings) =
            SensorInfo(
                id = tagEntity.id,
                rssi = tagEntity.rssi,
                name = sensorSettings.name,
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
    }
}