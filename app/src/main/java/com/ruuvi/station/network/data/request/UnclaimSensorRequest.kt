package com.ruuvi.station.network.data.request

data class UnclaimSensorRequest (
    val sensor: String,
    val deleteData: Boolean = false
)