package com.ruuvi.station.graph.model

import com.ruuvi.station.database.tables.TagSensorReading
import com.github.mikephil.charting.data.Entry
import com.ruuvi.station.units.domain.mould.MouldRiskCalculator
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.Segment
import com.ruuvi.station.vico.model.SegmentType

/** Readings already have calibration offsets applied by TagDetailsInteractor. */
fun mouldRiskHistory(readings: List<TagSensorReading>): ChartData {
    val segments = mutableListOf<Segment>()
    val timestamps = mutableListOf<Long>()
    val scores = mutableListOf<Double>()
    fun finishSegment() {
        if (scores.isNotEmpty()) {
            segments += Segment(timestamps.toList(), scores.toList(),
                if (scores.size == 1) SegmentType.Single else SegmentType.Solid)
        }
        timestamps.clear()
        scores.clear()
    }
    for (reading in readings) {
        val score = MouldRiskCalculator.calculate(reading.temperature, reading.humidity).score
        if (score == null) {
            finishSegment()
            continue
        }
        val timestamp = reading.createdAt.time
        if (timestamps.lastOrNull()?.let { timestamp - it > 3_600_000L } == true) finishSegment()
        timestamps += timestamp
        scores += score
    }
    finishSegment()
    return ChartData(segments, 0.0, 100.0)
}

/** Preserve the calculated value while MPAndroidChart uses floats for drawing. */
data class MouldRiskChartPoint(val score: Double, val startsSegment: Boolean)

val Entry.startsSegment: Boolean
    get() = (data as? MouldRiskChartPoint)?.startsSegment == true

val Entry.preciseValue: Double
    get() = (data as? MouldRiskChartPoint)?.score ?: y.toDouble()

fun ChartData.toMouldRiskEntries(from: Long): List<Entry> = segments.flatMap { segment ->
    segment.timestamps.zip(segment.values).mapIndexed { index, (time, score) ->
        Entry((time - from).toFloat(), score.toFloat(), MouldRiskChartPoint(score, index == 0))
    }
}
