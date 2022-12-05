package com.ruuvi.station.network.data.request

data class ContestSensorRequest (
    val sensor: String,
    val secret: String,
    val name: String
)