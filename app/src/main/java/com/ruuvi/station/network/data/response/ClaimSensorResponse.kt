package com.ruuvi.station.network.data.response

typealias ClaimSensorResponse = RuuviNetworkResponse<ClaimSensorResponseBody>

data class ClaimSensorResponseBody (
    val sensor: String
)