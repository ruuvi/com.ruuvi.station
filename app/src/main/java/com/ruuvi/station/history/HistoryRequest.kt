package com.ruuvi.station.history

import com.ruuvi.station.network.data.request.SensorDataMode

/** Keep mode and page size fixed throughout pagination, including the last short page. */
data class HistoryRequest(val range: HistoryRange, val mode: SensorDataMode, val limit: Int) {
    companion object {
        const val RECENT_WINDOW_MILLIS = 48 * 3_600_000L
        const val PAGE_SIZE = 5000
        const val LONG_RANGE_PAGE_SIZE = 144_000

        fun plan(missing: List<HistoryRange>, selected: HistoryRange, now: Long): List<HistoryRequest> {
            val limit = if (selected.endExclusiveMillis - selected.startMillis >= 90 * 86_400_000L)
                LONG_RANGE_PAGE_SIZE else PAGE_SIZE
            val split = (now - RECENT_WINDOW_MILLIS) / 1000 * 1000
            return missing.filterNot { it.isEmpty }.flatMap { range ->
                buildList {
                    // Mixed archive responses can combine a truncated sparse page with recent
                    // dense data. Advancing past the latter would silently skip archive pages.
                    if (range.startMillis < split) add(HistoryRequest(
                        HistoryRange(range.startMillis, minOf(split, range.endExclusiveMillis)), SensorDataMode.SPARSE, limit))
                    if (range.endExclusiveMillis > split) add(HistoryRequest(
                        HistoryRange(maxOf(split, range.startMillis), range.endExclusiveMillis), SensorDataMode.MIXED, limit))
                }
            }
        }
    }
}

/** Archive coverage stays reusable; opening rechecks 48 hours, live polling just one minute. */
fun HistoryRange.historyToFetch(coverage: List<HistoryRange>, revalidate: Boolean, now: Long): List<HistoryRange> {
    val cutoff = now - if (revalidate) HistoryRequest.RECENT_WINDOW_MILLIS else 60_000L
    val reusable = HistoryRange(startMillis, minOf(endExclusiveMillis, cutoff).coerceAtLeast(startMillis))
    return uncoveredHistory(this, coverage.map { it.intersect(reusable) })
}
