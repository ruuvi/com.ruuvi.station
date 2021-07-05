package com.ruuvi.station.network.data.response

typealias SetAlertResponse = RuuviNetworkResponse<SetAlertResponseBody>

data class SetAlertResponseBody (
    val action: String
)