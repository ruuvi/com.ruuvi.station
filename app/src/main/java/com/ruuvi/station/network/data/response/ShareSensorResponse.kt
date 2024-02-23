package com.ruuvi.station.network.data.response

typealias ShareSensorResponse = RuuviNetworkResponse<ShareSensorResponseBody>

data class ShareSensorResponseBody (
    val sensor: String,
    val invited: Boolean
)