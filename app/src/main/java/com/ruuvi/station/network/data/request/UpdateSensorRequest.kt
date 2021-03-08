package com.ruuvi.station.network.data.request

data class UpdateSensorRequest (
    val sensor: String,
    val name: String,
    val public: Boolean? = null
)