package com.ruuvi.station.network.data.request

data class PushUnregisterRequest(
    val token: String?,
    val id: Long? = null
)