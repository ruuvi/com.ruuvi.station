package com.ruuvi.station.network.data.request

import java.util.*

data class UpdateUserSettingRequest(
    val name: String,
    val value: String,
    val timestamp: Long = Date().time / 1000
)