package com.ruuvi.station.network.data.response

typealias ContestSensorResponse = RuuviNetworkResponse<ContestSensorResponseBody>

data class ContestSensorResponseBody (
    val sensor: String
)