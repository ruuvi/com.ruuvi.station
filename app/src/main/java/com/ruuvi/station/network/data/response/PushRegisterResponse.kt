package com.ruuvi.station.network.data.response

typealias PushRegisterResponse = RuuviNetworkResponse<PushRegisterResponseBody>

data class PushRegisterResponseBody(
    val tokenId: Long
)

typealias PushUnregisterResponse = RuuviNetworkResponse<Any>