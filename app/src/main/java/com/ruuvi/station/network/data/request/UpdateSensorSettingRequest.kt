package com.ruuvi.station.network.data.request

import com.ruuvi.station.network.domain.CloudClock
import java.util.Date

data class UpdateSensorSettingRequest(
    val sensor: String,
    val type: List<String>,
    val value: List<String>,
    val timestamp: Long = CloudClock.nowSeconds()
)
