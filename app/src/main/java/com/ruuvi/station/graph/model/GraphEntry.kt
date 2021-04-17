package com.ruuvi.station.graph.model

data class GraphEntry(
    val timestamp: Float,
    val temperature: Float,
    val humidity: Float?,
    val pressure: Float?
)