package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.tag.domain.RuuviTag.Companion.dataFormatIsAir
import com.ruuvi.station.util.MacAddressUtils
import java.util.*

@Table(
    name = "RuuviTag",
    database = LocalDatabase::class,
    useBooleanGetterSetters = false
)
data class RuuviTagEntity(
    @Column
    @PrimaryKey
    var id: String? = null,
    @Column
    var rssi: Int = 0,
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
    var favorite: Boolean = false,
    @Column
    var accelX: Double? = null,
    @Column
    var accelY: Double? = null,
    @Column
    var accelZ: Double? = null,
    @Column
    var voltage: Double = 0.0,
    @Column
    var updateAt: Date = Date(),
    @Column
    var dataFormat: Int = 0,
    @Column
    var txPower: Double = 0.0,
    @Column
    var movementCounter: Int? = null,
    @Column
    var measurementSequenceNumber: Int = 0,
    @Column
    var connectable: Boolean = false,
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
    var luminosity: Double? = null,
    @Column
    var dBaAvg: Double? = null,
    @Column
    var dBaPeak: Double? = null,
): BaseModel() {

    constructor(tag: FoundRuuviTag) :this(
        id = tag.id,
        rssi = tag.rssi ?: 0,
        temperature = tag.temperature,
        humidity = tag.humidity,
        pressure = tag.pressure,
        accelX = tag.accelX,
        accelY = tag.accelY,
        accelZ = tag.accelZ,
        voltage = tag.voltage ?: 0.0,
        dataFormat = tag.dataFormat ?: 0,
        txPower = tag.txPower ?: 0.0,
        movementCounter = tag.movementCounter,
        measurementSequenceNumber = tag.measurementSequenceNumber ?: 0,
        connectable = tag.connectable ?: false,
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
    )

    constructor(reading: TagSensorReading):this(
        id = reading.ruuviTagId,
        rssi = reading.rssi ?: 0,
        temperature = reading.temperature,
        humidity = reading.humidity,
        pressure = reading.pressure,
        accelX = reading.accelX,
        accelY = reading.accelY,
        accelZ = reading.accelZ,
        voltage = reading.voltage ?: 0.0,
        dataFormat = reading.dataFormat,
        txPower = reading.txPower ?: 0.0,
        pm1 = reading.pm1,
        pm25 = reading.pm25,
        pm4 = reading.pm4,
        pm10 = reading.pm10,
        co2 = reading.co2,
        voc = reading.voc,
        nox = reading.nox,
        luminosity = reading.luminosity,
        dBaAvg = reading.dBaAvg,
        dBaPeak = reading.dBaPeak,
        movementCounter = reading.movementCounter,
        measurementSequenceNumber = reading.measurementSequenceNumber ?: 0,
        temperatureOffset = reading.temperatureOffset,
        humidityOffset = reading.humidityOffset,
        pressureOffset = reading.pressureOffset,
        updateAt = reading.createdAt,
    )

    fun displayName(): String = id?.let { MacAddressUtils.getDefaultName(it, isAir()) } ?: ""

    fun preserveData(tag: RuuviTagEntity) {
        tag.favorite = favorite
        tag.updateAt = Date()
    }

    fun updateData(reading: TagSensorReading) {
        rssi = reading.rssi ?: 0
        temperature = reading.temperature
        humidity = reading.humidity
        pressure = reading.pressure
        accelX = reading.accelX
        accelY = reading.accelY
        accelZ = reading.accelZ
        voltage = reading.voltage ?: 0.0
        dataFormat = reading.dataFormat
        txPower = reading.txPower ?: 0.0
        movementCounter = reading.movementCounter
        measurementSequenceNumber = reading.measurementSequenceNumber ?: 0
        updateAt = reading.createdAt
        temperatureOffset = reading.temperatureOffset
        humidityOffset = reading.humidityOffset
        pressureOffset = reading.pressureOffset
        pm1 = reading.pm1
        pm25 = reading.pm25
        pm4 = reading.pm4
        pm10 = reading.pm10
        co2 = reading.co2
        voc = reading.voc
        nox = reading.nox
        luminosity = reading.luminosity
        dBaAvg = reading.dBaAvg
        dBaPeak = reading.dBaPeak
    }

    companion object {
        val queryFields = arrayOf(
            RuuviTagEntity_Table.id.withTable(),
            RuuviTagEntity_Table.rssi.withTable(),
            RuuviTagEntity_Table.temperature.withTable(),
            RuuviTagEntity_Table.temperatureOffset.withTable(),
            RuuviTagEntity_Table.humidity.withTable(),
            RuuviTagEntity_Table.humidityOffset.withTable(),
            RuuviTagEntity_Table.pressure.withTable(),
            RuuviTagEntity_Table.pressureOffset.withTable(),
            RuuviTagEntity_Table.favorite.withTable(),
            RuuviTagEntity_Table.accelX.withTable(),
            RuuviTagEntity_Table.accelY.withTable(),
            RuuviTagEntity_Table.accelZ.withTable(),
            RuuviTagEntity_Table.voltage.withTable(),
            RuuviTagEntity_Table.updateAt.withTable(),
            RuuviTagEntity_Table.dataFormat.withTable(),
            RuuviTagEntity_Table.txPower.withTable(),
            RuuviTagEntity_Table.pm1.withTable(),
            RuuviTagEntity_Table.pm25.withTable(),
            RuuviTagEntity_Table.pm4.withTable(),
            RuuviTagEntity_Table.pm10.withTable(),
            RuuviTagEntity_Table.co2.withTable(),
            RuuviTagEntity_Table.voc.withTable(),
            RuuviTagEntity_Table.nox.withTable(),
            RuuviTagEntity_Table.luminosity.withTable(),
            RuuviTagEntity_Table.dBaAvg.withTable(),
            RuuviTagEntity_Table.dBaPeak.withTable(),
            RuuviTagEntity_Table.movementCounter.withTable(),
            RuuviTagEntity_Table.measurementSequenceNumber.withTable(),
            RuuviTagEntity_Table.connectable.withTable()
        )
    }
}

fun RuuviTagEntity.isLowBattery(): Boolean {
    val temperature = this.temperature ?: return this.voltage < 2.5
    return when {
        temperature <= -20 && this.voltage < 2 && this.voltage > 0 -> true
        temperature > -20 && temperature < 0 && this.voltage < 2.3 && this.voltage > 0 -> true
        temperature >= 0 && this.voltage < 2.5 && this.voltage > 0 -> true
        else -> false
    }
}

fun RuuviTagEntity.isAir(): Boolean = dataFormatIsAir(this.dataFormat)