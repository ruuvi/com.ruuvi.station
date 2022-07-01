package com.ruuvi.station.network.data.request

data class SensorDenseRequest(
    val sensor: String? = null,
    val sharedToOthers: Boolean = false,
    val sharedToMe: Boolean = false,
    val measurements: Boolean = false,
    val alerts: Boolean = false
)