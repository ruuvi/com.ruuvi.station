package com.ruuvi.station.network.data.request

import com.ruuvi.station.network.domain.CloudClock
import java.util.*

data class UpdateUserSettingRequest(
    val name: String,
    val value: String,
    val timestamp: Long = CloudClock.nowSeconds()
)
