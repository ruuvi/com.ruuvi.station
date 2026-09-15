package com.ruuvi.station.history

data class HistoryPoint(val timestamp: Long, val value: Double, val segment: Int)

data class HistoryStatistics(
    val count: Long, val minimum: Double, val maximum: Double,
    val average: Double, val latest: Double
)

data class SampledHistory(val points: List<HistoryPoint>, val statistics: HistoryStatistics?)

/** One pass over ordered raw measurements; memory and output are bounded per measurement type. */
class HistorySampler(private val range: HistoryRange) {
    private val small = ArrayList<HistoryPoint>(MAX_POINTS)
    private val minima = arrayOfNulls<HistoryPoint>(BUCKETS)
    private val maxima = arrayOfNulls<HistoryPoint>(BUCKETS)
    private var first: HistoryPoint? = null
    private var last: HistoryPoint? = null
    private var count = 0L
    private var minimum = Double.POSITIVE_INFINITY
    private var maximum = Double.NEGATIVE_INFINITY
    private var area = 0.0

    fun add(timestamp: Long, value: Double?) {
        if (value == null || !value.isFinite() || timestamp < range.startMillis || timestamp >= range.endExclusiveMillis) return
        val previous = last
        require(previous == null || timestamp >= previous.timestamp) { "History must be ordered" }
        val point = HistoryPoint(timestamp, value,
            (previous?.segment ?: 0) + if (previous != null && timestamp - previous.timestamp > GAP_MILLIS) 1 else 0)
        if (first == null) first = point
        if (previous != null) area += (timestamp - previous.timestamp) * (previous.value + value) / 2.0
        last = point
        count++
        minimum = minOf(minimum, value)
        maximum = maxOf(maximum, value)
        if (count <= MAX_POINTS) small.add(point) else small.clear()
        val bucket = ((timestamp - range.startMillis).toDouble() /
            (range.endExclusiveMillis - range.startMillis) * BUCKETS).toInt().coerceIn(0, BUCKETS - 1)
        if (minima[bucket] == null || value < minima[bucket]!!.value) minima[bucket] = point
        if (maxima[bucket] == null || value > maxima[bucket]!!.value) maxima[bucket] = point
    }

    fun finish(): SampledHistory {
        val start = first ?: return SampledHistory(emptyList(), null)
        val end = last!!
        val points = if (count <= MAX_POINTS) small.toList() else
            (listOf(start, end) + minima.filterNotNull() + maxima.filterNotNull())
                .distinct().sortedBy { it.timestamp }
        val duration = end.timestamp - start.timestamp
        return SampledHistory(points, HistoryStatistics(count, minimum, maximum,
            if (duration == 0L) end.value else area / duration, end.value))
    }

    companion object {
        const val MAX_POINTS = 1000
        const val GAP_MILLIS = 3_600_000L
        private const val BUCKETS = (MAX_POINTS - 2) / 2
    }
}
