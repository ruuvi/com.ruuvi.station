package com.ruuvi.station.widgets.data

data class SimpleWidgetData(
    val sensorId: String,
    val displayName: String,
    val sensorValue: String,
    val unit: String,
    val updated: String?
)