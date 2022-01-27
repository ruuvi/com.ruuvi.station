package com.ruuvi.station.widgets.data

import java.util.*

data class WidgetData(
    val sensorId: String,
    val displayName:String = "",
    val temperature: String = "",
    val humidity: String = "",
    val pressure: String = "",
    val movement: String = "",
    val updatedAt: Date? = null
)