package com.ruuvi.station.network.data.response

data class RuuviNetworkResponse<T> (
    val result: String,
    val error: String,
    val data: T
)