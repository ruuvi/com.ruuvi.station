package com.ruuvi.station.vico.model

data class ChartData(
    val segments: List<Segment>,
    val minValue: Double,
    val maxValue: Double
)

data class Segment(
    val timestamps: List<Long>,
    val values: List<Double>,
    val segmentType: SegmentType
)

sealed class SegmentType() {
    object Solid: SegmentType()
    object Dotted: SegmentType()
    object Single: SegmentType()
}