package com.ruuvi.station.export


data class ExportColumn(
    val name: String,
    val width: Double? = null
)

data class ExportData(
    val sensorName: String,
    val columns: List<ExportColumn>,
    val rows: List<List<Any?>>
)