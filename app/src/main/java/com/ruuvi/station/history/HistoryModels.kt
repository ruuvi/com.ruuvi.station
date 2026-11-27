package com.ruuvi.station.history

import java.io.IOException
import kotlin.math.max
import kotlin.math.min

/** Whole-second, inclusive bounds throughout the transport, cache and export pipeline. */
data class HistoryWindow(val since: Long, val until: Long) {
    init { require(since >= 0 && until >= since && until <= 253402300799L && until - since <= MAX_SPAN) }
    val seconds get() = until - since + 1
    operator fun contains(time: Long) = time in since..until
    fun intersect(other: HistoryWindow): HistoryWindow? =
        if (max(since, other.since) <= min(until, other.until))
            HistoryWindow(max(since, other.since), min(until, other.until)) else null
    companion object {
        const val MAX_DAYS = 1096
        const val MAX_SPAN = MAX_DAYS * 86400L
    }
}

enum class HistoryMode { DENSE, SPARSE }
data class HistoryQuery(val sensor: String, val window: HistoryWindow, val mode: HistoryMode) {
    init { require(sensor.matches(Regex("[0-9A-F]{2}(:[0-9A-F]{2}){5}"))) }
    fun validate(page: HistoryPage) {
        if (page.sensor != sensor || page.readings.size > PAGE_SIZE) throw HistoryFailure.Invalid()
        var previous = window.until + 1
        for (row in page.readings) {
            if (row.timestamp !in window || row.timestamp >= previous || row.data.isEmpty() ||
                row.data.length > 2048 || row.data.length % 2 != 0 ||
                row.data.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' } ||
                row.rssi?.let { it !in -128..127 } == true) throw HistoryFailure.Invalid()
            previous = row.timestamp
        }
    }
    companion object { const val PAGE_SIZE = 1500 }
}
data class HistoryReading(val timestamp: Long, val data: String, val rssi: Int?)
data class HistoryPage(val sensor: String, val readings: List<HistoryReading>)
data class HistoryValues(val timestamp: Long, val values: Map<String, Double>)
data class HistorySample(val timestamp: Long, val value: Double, val segment: Int)
data class HistoryStatistics(val minimum: Double, val maximum: Double, val average: Double)
data class HistoryPlot(
    val window: HistoryWindow,
    val samples: Map<String, List<HistorySample>> = emptyMap(),
    val statistics: Map<String, HistoryStatistics> = emptyMap(),
    val coverage: List<HistoryWindow> = emptyList(),
    val complete: Boolean = false,
    val readings: Long = 0,
    val statisticsPartial: Boolean = false,
)
data class HistoryProgress(val plot: HistoryPlot, val fraction: Float, val networkPages: Int, val cachedPages: Int, val downloading: Boolean = false, val preview: Boolean = false)
data class HistoryRefresh(val since: Long, val startedMillis: Long,
                          val networkSince: Long? = null) {
    companion object {
        fun opening(nowMillis: Long) = HistoryRefresh(Long.MAX_VALUE, nowMillis)
        fun live(nowMillis: Long) = HistoryRefresh.opening(nowMillis)
    }
}

class HistoryRanges {
    var ranges: List<HistoryWindow> = emptyList(); private set
    fun insert(window: HistoryWindow) {
        val merged = mutableListOf<HistoryWindow>()
        for (item in (ranges + window).sortedBy { it.since }) {
            val last = merged.lastOrNull()
            if (last != null && item.since <= last.until + 1)
                merged[merged.lastIndex] = HistoryWindow(last.since, max(last.until, item.until))
            else merged += item
        }
        ranges = merged
    }
    fun missing(window: HistoryWindow): List<HistoryWindow> {
        val gaps = mutableListOf<HistoryWindow>()
        var cursor = window.since
        for (item in ranges) {
            if (item.until < cursor || item.since > window.until) continue
            if (item.since > cursor) gaps += HistoryWindow(cursor, min(window.until, item.since - 1))
            cursor = max(cursor, item.until + 1)
            if (cursor > window.until) return gaps
        }
        if (cursor <= window.until) gaps += HistoryWindow(cursor, window.until)
        return gaps
    }
}

sealed class HistoryFailure(message: String) : Exception(message) {
    class Invalid : HistoryFailure("Invalid cloud history response.")
    class Access : HistoryFailure("Cloud history access is no longer available.")
    class Http(val status: Int, val retryAfterMillis: Long? = null) : HistoryFailure("Cloud history request failed ($status).")
}

/** Values are reduced from actual readings, never the extrema-selected drawing points. */
enum class HistoryStatisticsPhase { READY, UPDATING, PARTIAL, FAILED }
data class HistoryStatisticsDisplay(val values: HistoryStatistics?, val phase: HistoryStatisticsPhase)
