package com.ruuvi.station.graph.model

import androidx.compose.runtime.mutableStateListOf
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry

data class ChartContainer (
    val chartSensorType: ChartSensorType,
    var uiComponent: LineChart?,
    var data: MutableList<Entry>? = mutableStateListOf(),
    var limits: Pair<Double, Double>? = null,
    var from: Long? = null,
    var to: Long? = null
)