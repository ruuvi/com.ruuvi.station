package com.ruuvi.station.gateway.data

import java.util.*

open class Event (
    val deviceId: String,
    val location: ScanLocation?,
    val batteryLevel: Int?
) {
    val time: Date = Date()
    val eventId: String = UUID.randomUUID().toString()
}