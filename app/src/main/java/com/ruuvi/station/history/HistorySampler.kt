package com.ruuvi.station.history

import kotlin.math.max
import kotlin.math.min

/** Port of iOS CloudHistorySampler: actual endpoints/extrema, raw statistics, true gap identity. */
class HistorySampler(private val window: HistoryWindow, private val metrics: List<String>) {
    private data class Point(val time: Long, val value: Double)
    private data class Piece(val first: Point, var last: Point = first, var min: Double = first.value,
                             var max: Double = first.value, var area: Double = 0.0, var count: Int = 1) {
        fun add(point: Point) {
            area += (point.time - last.time).toDouble() * (last.value + point.value) / 2
            min = min(min, point.value); max = max(max, point.value); last = point; count++
        }
    }
    private class Reduction {
        val low = mutableMapOf<Int, Point>()
        val high = mutableMapOf<Int, Point>()
        var small: MutableList<Point>? = mutableListOf()
        val pieces = mutableListOf<Piece>()
        val gapEnds = mutableListOf<Long>()
        val gapEdges = mutableListOf<Point>()
        fun add(points: List<Point>, window: HistoryWindow) {
            val first = points.firstOrNull() ?: return
            val piece = Piece(first)
            points.forEachIndexed { index, point ->
                if (index > 0) {
                    if (point.time - piece.last.time > 3600) {
                        gapEnds += point.time
                        if (gapEdges.size < MAX_POINTS - 4) gapEdges += listOf(piece.last, point)
                    }
                    piece.add(point)
                }
                val bin = min(BUCKETS - 1, ((point.time - window.since).toDouble() / window.seconds * BUCKETS).toInt())
                val oldLow = low[bin]; val oldHigh = high[bin]
                if (oldLow == null || point.value < oldLow.value || point.value == oldLow.value && point.time < oldLow.time) low[bin] = point
                if (oldHigh == null || point.value > oldHigh.value || point.value == oldHigh.value && point.time > oldHigh.time) high[bin] = point
                small?.let { if (it.size < MAX_POINTS) it += point else small = null }
            }
            pieces += piece
        }
        fun finish(): Pair<List<HistorySample>, HistoryStatistics>? {
            val ordered = pieces.sortedBy { it.first.time }
            val first = ordered.firstOrNull()?.first ?: return null
            val last = ordered.last().last
            val ends = gapEnds.toMutableList(); val edges = gapEdges.toMutableList()
            var area = 0.0; var minimum = Double.POSITIVE_INFINITY; var maximum = Double.NEGATIVE_INFINITY
            var previous: Point? = null
            for (piece in ordered) {
                previous?.let {
                    area += (piece.first.time - it.time).toDouble() * (it.value + piece.first.value) / 2
                    if (piece.first.time - it.time > 3600) {
                        ends += piece.first.time
                        if (edges.size < MAX_POINTS - 4) edges += listOf(it, piece.first)
                    }
                }
                area += piece.area; minimum = min(minimum, piece.min); maximum = max(maximum, piece.max)
                previous = piece.last
            }
            val extrema = low.values + high.values
            val retained = linkedMapOf<Long, Point>()
            val smallPoints = small
            if (smallPoints != null) smallPoints.forEach { retained[it.time] = it }
            else {
                val byValue = compareBy<Point> { it.value }.thenBy { it.time }
                (listOf(first, last) + listOfNotNull(extrema.minWithOrNull(byValue), extrema.maxWithOrNull(byValue)))
                    .forEach { retained[it.time] = it }
                if (ends.size * 2 < MAX_POINTS - 4) edges.forEach { retained[it.time] = it }
                val candidates = extrema.filter { !retained.containsKey(it.time) }.sortedBy { it.time }
                val capacity = min(MAX_POINTS - retained.size, candidates.size)
                repeat(capacity) { val point = candidates[it * candidates.size / capacity]; retained[point.time] = point }
            }
            ends.sort()
            var segment = 0
            val samples = retained.values.sortedBy { it.time }.map {
                while (segment < ends.size && ends[segment] <= it.time) segment++
                HistorySample(it.time, it.value, segment)
            }
            val average = if (last.time > first.time) area / (last.time - first.time) else last.value
            return samples to HistoryStatistics(minimum, maximum, average)
        }
    }
    private val reductions = metrics.associateWith { Reduction() }
    var readings = 0L; private set
    fun addPage(rows: List<HistoryValues>) {
        val ordered = rows.filter { it.timestamp in window }.sortedBy { it.timestamp }
        readings += ordered.size
        metrics.forEach { metric ->
            reductions.getValue(metric).add(ordered.mapNotNull { row ->
                row.values[metric]?.takeIf { it.isFinite() }?.let { Point(row.timestamp, it) }
            }, window)
        }
    }
    fun plot(coverage: List<HistoryWindow>, complete: Boolean): HistoryPlot {
        val finished = reductions.mapNotNull { (metric, reduction) -> reduction.finish()?.let { metric to it } }.toMap()
        return HistoryPlot(window, finished.mapValues { it.value.first }, finished.mapValues { it.value.second }, coverage, complete, readings)
    }
    companion object { const val MAX_POINTS = 400; private const val BUCKETS = (MAX_POINTS - 2) / 2 }
}
