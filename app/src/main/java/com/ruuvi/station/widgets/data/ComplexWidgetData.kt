package com.ruuvi.station.widgets.data

import java.util.Date

data class ComplexWidgetData(
    val sensorId: String,
    val timestamp: Date,
    var displayName: String,
    var sensorValues: List<SensorValue>,
    var updated: String?
)

data class SensorValue(
    val type: WidgetType,
    val sensorValue: String,
    val unit: String
)