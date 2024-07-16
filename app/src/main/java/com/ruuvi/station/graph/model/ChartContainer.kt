package com.ruuvi.station.graph.model

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry

data class ChartContainer (
    val chartSensorType: ChartSensorType,
    val uiComponent: LineChart,
    val data: MutableList<Entry>,
    val limits: Pair<Double, Double>?,
    val from: Long,
    val to: Long
)