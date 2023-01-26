package com.ruuvi.station.network.data.response

typealias PushRegisterResponse = RuuviNetworkResponse<PushRegisterResponseBody>

data class PushRegisterResponseBody(
    val id: Long,
    val lastAccessed: Long,
    val name: String
)

typealias PushUnregisterResponse = RuuviNetworkResponse<Any>