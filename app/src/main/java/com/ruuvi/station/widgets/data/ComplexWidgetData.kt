package com.ruuvi.station.widgets.data

data class ComplexWidgetData(
    val sensorId: String,
    var displayName: String,
    var sensorValues: List<SensorValue>,
    var updated: String?
)

data class SensorValue(
    val type: WidgetType,
    val sensorValue: String,
    val unit: String
)