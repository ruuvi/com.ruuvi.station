package com.ruuvi.station.network.data.request

data class ContestSensorRequest (
    val macAddress: String,
    val secret: String,
    val name: String
)