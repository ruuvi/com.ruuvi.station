package com.ruuvi.station.bluetooth.interfaces

import java.util.Date

class IRuuviTag {
    var id: String? = null
    var url: String? = null
    var rssi: Int? = null
    var data: DoubleArray? = null
    var name: String? = null
    var temperature: Double? = null
    var humidity: Double? = null
    var pressure: Double? = null
    var favorite: Boolean = false
    var accelX: Double? = null
    var accelY: Double? = null
    var accelZ: Double? = null
    var voltage: Double? = null
    var updateAt: Date? = null
    var gatewayUrl: String? = null
    var defaultBackground: Int? = null
    var userBackground: String? = null
    var dataFormat: Int? = null
    var txPower: Double? = null
    var movementCounter: Int? = null
    var measurementSequenceNumber: Int? = null
    val displayName: String? = null
}