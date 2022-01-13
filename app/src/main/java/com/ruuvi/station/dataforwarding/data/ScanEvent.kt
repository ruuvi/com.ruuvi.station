package com.ruuvi.station.dataforwarding.data

class ScanEvent(
    deviceId: String,
    location: ScanLocation?,
    batteryLevel: Int?
): Event(deviceId, location, batteryLevel) {
    val tags = mutableListOf<SensorInfo>()
}