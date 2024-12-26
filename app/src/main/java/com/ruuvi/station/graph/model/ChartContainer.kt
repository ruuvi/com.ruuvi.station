package com.ruuvi.station.graph.model

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry

data class ChartContainer (
    val chartSensorType: ChartSensorType,
    val uiComponent: LineChart?,
    var data: MutableList<Entry>? = null,
    var limits: Pair<Double, Double>? = null,
    var from: Long? = null,
    var to: Long? = null
)