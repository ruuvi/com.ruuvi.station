package com.ruuvi.station.network.data.response

typealias CheckSensorResponse = RuuviNetworkResponse<CheckSensorResponseBody>

data class CheckSensorResponseBody (
    val email: String
)