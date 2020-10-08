package com.ruuvi.station.network.data.response

typealias ShareSensorResponse = RuuviNetworkResponse<ShareSensorDataResponse>

data class ShareSensorDataResponse (
    val sensor: String
)