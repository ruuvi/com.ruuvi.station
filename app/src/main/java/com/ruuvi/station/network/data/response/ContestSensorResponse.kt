package com.ruuvi.station.network.data.response

typealias ContestSensorResponse = RuuviNetworkResponse<ClaimSensorResponseBody>

data class ContestSensorResponseBody (
    val sensor: String
)