package com.ruuvi.station.widgets.data

import java.util.Date

data class SimpleWidgetData(
    val sensorId: String,
    val timestamp: Date,
    val displayName: String,
    val sensorValue: String,
    val unit: String,
    val updated: String?
)