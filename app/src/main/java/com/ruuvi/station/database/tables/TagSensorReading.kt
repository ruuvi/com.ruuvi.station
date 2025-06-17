package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.*
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.bluetooth.contract.LogReading
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
    var temperature: Double? = null,
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
    var rssi: Int? = null,
    @Column
    var accelX: Double? = null,
    @Column
    var accelY: Double? = null,
    @Column
    var accelZ: Double? = null,
    @Column
    var voltage: Double? = null,
    @Column
    var dataFormat: Int = 0,
    @Column
    var txPower: Double? = null,
    @Column
    var movementCounter: Int? = null,
    @Column
    var measurementSequenceNumber: Int? = null,
    @Column
    var pm1: Double? = null,
    @Column
    var pm25: Double? = null,
    @Column
    var pm4: Double? = null,
    @Column
    var pm10: Double? = null,
    @Column
    var co2: Int? = null,
    @Column
    var voc: Int? = null,
    @Column
    var nox: Int? = null,
    @Column
    var luminosity: Int? = null,
    @Column
    var dBaAvg: Double? = null,
    @Column
    var dBaPeak: Double? = null,
): BaseModel() {
    constructor(tag: RuuviTagEntity) : this(
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
        pm1 = tag.pm1,
        pm25 = tag.pm25,
        pm4 = tag.pm4,
        pm10 = tag.pm10,
        co2 = tag.co2,
        voc = tag.voc,
        nox = tag.nox,
        luminosity = tag.luminosity,
        dBaAvg = tag.dBaAvg,
        dBaPeak = tag.dBaPeak,
        movementCounter = tag.movementCounter,
        measurementSequenceNumber = tag.measurementSequenceNumber,
        createdAt = Date()
    )

    constructor(tag: FoundRuuviTag, timestamp: Date) : this(
        ruuviTagId = tag.id,
        temperature = tag.temperature,
        humidity = tag.humidity,
        pressure = tag.pressure,
        rssi = tag.rssi ?: 0,
        accelX = tag.accelX,
        accelY = tag.accelY,
        accelZ = tag.accelZ,
        voltage = tag.voltage,
        dataFormat = tag.dataFormat ?: 0,
        txPower = tag.txPower,
        pm1 = tag.pm1,
        pm25 = tag.pm25,
        pm4 = tag.pm4,
        pm10 = tag.pm10,
        co2 = tag.co2,
        voc = tag.voc,
        nox = tag.nox,
        luminosity = tag.luminosity,
        dBaAvg = tag.dBaAvg,
        dBaPeak = tag.dBaPeak,
        movementCounter = tag.movementCounter,
        measurementSequenceNumber = tag.measurementSequenceNumber,
        createdAt = timestamp
    )

    constructor(log: LogReading) : this(
        ruuviTagId = log.id,
        createdAt = log.date,
        temperature = log.temperature,
        humidity = log.humidity,
        pressure = log.pressure,
        pm1 = log.pm1,
        pm25 = log.pm25,
        pm4 = log.pm4,
        pm10 = log.pm10,
        co2 = log.co2,
        voc = log.voc,
        nox = log.nox,
        luminosity = log.luminosity,
        dBaAvg = log.dBaAvg,
        dBaPeak = log.dBaPeak,
        voltage = log.voltage,
        dataFormat = log.dataFormat ?: 0
    )
}