package com.ruuvi.station.history

/** Timer updates may read only the new tail. An evicted raw archive must not erase the retained plot. */
fun HistoryPlot.preservingLoadedHistory(previous: HistoryPlot): HistoryPlot {
    if (!statisticsPartial) return this
    val combinedCoverage = HistoryRanges()
    (previous.coverage + coverage).mapNotNull { it.intersect(window) }.forEach(combinedCoverage::insert)
    val merged = (previous.samples.keys + samples.keys).associateWith { metric ->
        val old = previous.samples[metric].orEmpty().filter { it.timestamp in window }
        val tail = samples[metric].orEmpty().filter { it.timestamp > previous.window.until }
        val offset = if (old.isEmpty() || tail.isEmpty()) 0 else
            old.last().segment + (if (tail.first().timestamp - old.last().timestamp > 3600) 1 else 0) - tail.first().segment
        reduceLiveSamples(old + tail.map { it.copy(segment = it.segment + offset) }, window)
    }
    return copy(samples = merged, coverage = combinedCoverage.ranges)
}

private fun reduceLiveSamples(points: List<HistorySample>, window: HistoryWindow): List<HistorySample> {
    if (points.size <= HistorySampler.MAX_POINTS) return points
    val bins = (HistorySampler.MAX_POINTS - 2) / 2
    val selected = linkedMapOf<Long, HistorySample>()
    selected[points.first().timestamp] = points.first(); selected[points.last().timestamp] = points.last()
    points.groupBy { ((it.timestamp - window.since).toDouble() / window.seconds * bins).toInt().coerceIn(0, bins - 1) }
        .values.forEach { group ->
            val low = group.minBy { it.value }; val high = group.maxBy { it.value }
            selected[low.timestamp] = low; selected[high.timestamp] = high
        }
    return selected.values.sortedBy { it.timestamp }
}
