package com.ruuvi.station.network.data.response

typealias PushListResponse = RuuviNetworkResponse<PushListResponseBody>

data class PushListResponseBody(
    val tokens: List<PushListElement>
)

data class PushListElement(
    val tokenId: Long,
    val name: String,
    val lastAccessed: Long
)