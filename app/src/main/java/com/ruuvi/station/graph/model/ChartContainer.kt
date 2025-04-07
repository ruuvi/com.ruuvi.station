package com.ruuvi.station.graph.model

import androidx.compose.runtime.mutableStateListOf
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.ruuvi.station.units.model.UnitType

data class ChartContainer (
    val unitType: UnitType,
    var uiComponent: LineChart?,
    var data: MutableList<Entry>? = mutableStateListOf(),
    var limits: Pair<Double, Double>? = null,
    var from: Long? = null,
    var to: Long? = null
)