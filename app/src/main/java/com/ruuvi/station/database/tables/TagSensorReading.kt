package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.*
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.bluetooth.FoundRuuviTag
import com.ruuvi.station.database.domain.LocalDatabase
import java.util.*

@Table(
    database = LocalDatabase::class,
    indexGroups = [IndexGroup (number = 1, name = "TagId")])
data class TagSensorReading(
    @PrimaryKey(autoincrement = true)
    @Column
    var id: Int = 0,
    @Index(indexGroups = [1])
    @Column
    var ruuviTagId: String? = null,
    @Index(indexGroups = [1])
    @Column
    var createdAt: Date = Date(),
    @Column
    var temperature: Double = 0.0,
    @Column
    var temperatureOffset: Double = 0.0,
    @Column
    var humidity: Double? = null,
    @Column
    var humidityOffset: Double = 0.0,
    @Column
    var pressure: Double? = null,
    @Column
    var pressureOffset: Double = 0.0,
    @Column
    var rssi: Int = 0,
    @Column
    var accelX: Double? = null,
    @Column
    var accelY: Double? = null,
    @Column
    var accelZ: Double? = null,
    @Column
    var voltage: Double = 0.0,
    @Column
    var dataFormat: Int = 0,
    @Column
    var txPower: Double = 0.0,
    @Column
    var movementCounter: Int? = null,
    @Column
    var measurementSequenceNumber: Int = 0
): BaseModel() {
    constructor(tag: RuuviTagEntity): this(
        ruuviTagId = tag.id!!,
        temperature = tag.temperature,
        temperatureOffset = tag.temperatureOffset,
        humidity = tag.humidity,
        humidityOffset = tag.humidityOffset,
        pressure = tag.pressure,
        pressureOffset = tag.pressureOffset,
        rssi = tag.rssi,
        accelX = tag.accelX,
        accelY = tag.accelY,
        accelZ = tag.accelZ,
        voltage = tag.voltage,
        dataFormat = tag.dataFormat,
        txPower = tag.txPower,
        movementCounter = tag.movementCounter,
        measurementSequenceNumber = tag.measurementSequenceNumber,
        createdAt = Date()
    )

    constructor(tag: FoundRuuviTag, timestamp: Date): this(
        ruuviTagId = tag.id,
        temperature = tag.temperature ?: 0.0,
        humidity = tag.humidity,
        pressure = tag.pressure,
        rssi = tag.rssi ?: 0,
        accelX = tag.accelX,
        accelY = tag.accelY,
        accelZ = tag.accelZ,
        voltage = tag.voltage ?: 0.0,
        dataFormat = tag.dataFormat ?: 0,
        txPower = tag.txPower ?: 0.0,
        movementCounter = tag.movementCounter ?: 0,
        measurementSequenceNumber = tag.measurementSequenceNumber ?: 0,
        createdAt = timestamp
    )
}