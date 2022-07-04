package com.ruuvi.station.widgets.data

import com.ruuvi.station.bluetooth.FoundRuuviTag
import java.util.*

data class DecodedSensorData(
    val id: String? = null,
    val rssi: Int? = null,
    val temperature: Double? = null,
    val humidity: Double? = null,
    val pressure: Double? = null,
    val accelX: Double? = null,
    val accelY: Double? = null,
    val accelZ: Double? = null,
    val voltage: Double? = null,
    val dataFormat: Int? = null,
    val txPower: Double? = null,
    val movementCounter: Int? = null,
    val measurementSequenceNumber: Int? = null,
    val connectable: Boolean? = null,
    val updatedAt: Date
) {
    constructor(foundRuuviTag: FoundRuuviTag, updatedAt: Date): this(
        id = foundRuuviTag.id,
        rssi = foundRuuviTag.rssi,
        temperature = foundRuuviTag.temperature,
        humidity = foundRuuviTag.humidity,
        pressure = foundRuuviTag.pressure,
        accelX = foundRuuviTag.accelX,
        accelY = foundRuuviTag.accelY,
        accelZ = foundRuuviTag.accelZ,
        voltage = foundRuuviTag.voltage,
        dataFormat = foundRuuviTag.dataFormat,
        txPower = foundRuuviTag.txPower,
        movementCounter = foundRuuviTag.movementCounter,
        measurementSequenceNumber = foundRuuviTag.measurementSequenceNumber,
        connectable = foundRuuviTag.connectable,
        updatedAt = updatedAt
    )
}
