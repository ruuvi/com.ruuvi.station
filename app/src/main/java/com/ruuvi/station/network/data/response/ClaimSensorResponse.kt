package com.ruuvi.station.network.data.response

typealias ClaimSensorResponse = RuuviNetworkResponse<ClaimSensorDataResponse>

data class ClaimSensorDataResponse (
    val sensor: String
)