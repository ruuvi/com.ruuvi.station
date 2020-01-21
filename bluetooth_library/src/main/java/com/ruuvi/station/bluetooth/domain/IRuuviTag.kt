package com.ruuvi.station.bluetooth.domain

import java.util.Date

interface IRuuviTag {
    var id: String?
    var url: String?
    var rssi: Int
    var data: DoubleArray?
    var name: String?
    var temperature: Double
    var humidity: Double
    var pressure: Double
    var favorite: Boolean
    var rawDataBlob: ByteArray?
    var rawData: ByteArray?
    var accelX: Double
    var accelY: Double
    var accelZ: Double
    var voltage: Double
    var updateAt: Date?
    var gatewayUrl: String?
    var defaultBackground: Int
    var userBackground: String?
    var dataFormat: Int
    var txPower: Double
    var movementCounter: Int
    var measurementSequenceNumber: Int
    val dispayName: String?
}