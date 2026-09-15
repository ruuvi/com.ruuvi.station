package com.ruuvi.station.history

import com.ruuvi.station.app.preferences.GlobalSettings
import java.util.Calendar
import java.util.TimeZone

/** All local ranges are [startMillis, endExclusiveMillis). */
data class HistoryRange(val startMillis: Long, val endExclusiveMillis: Long) {
    init { require(startMillis <= endExclusiveMillis) }
    val isEmpty: Boolean get() = startMillis == endExclusiveMillis

    fun intersect(other: HistoryRange): HistoryRange {
        val start = maxOf(startMillis, other.startMillis)
        return HistoryRange(start, maxOf(start, minOf(endExclusiveMillis, other.endExclusiveMillis)))
    }

    companion object {
        fun retained(now: Long) = HistoryRange(now - GlobalSettings.historyLengthMillis, now)
    }
}

/** Date picker timestamps encode calendar dates at UTC midnight, not local instants. */
sealed interface HistorySelection {
    data class Rolling(val hours: Int = 0) : HistorySelection
    data class Custom(val startDateUtc: Long, val endDateUtc: Long) : HistorySelection {
        init { require(startDateUtc <= endDateUtc) }
    }

    fun resolve(now: Long, zone: TimeZone = TimeZone.getDefault()): HistoryRange {
        val retained = HistoryRange.retained(now)
        return when (this) {
            is Rolling -> HistoryRange(
                now - (if (hours == 0) GlobalSettings.historyLengthHours else hours.coerceIn(1, GlobalSettings.historyLengthHours)) * 3_600_000L,
                now
            )
            is Custom -> HistoryRange(
                localMidnight(startDateUtc, zone),
                Calendar.getInstance(zone).apply {
                    timeInMillis = localMidnight(endDateUtc, zone)
                    add(Calendar.DATE, 1)
                }.timeInMillis
            ).intersect(retained)
        }
    }
}

fun localMidnight(dateUtc: Long, zone: TimeZone): Long {
    val date = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = dateUtc }
    return Calendar.getInstance(zone).apply {
        clear()
        set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun calendarDateUtc(instant: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val local = Calendar.getInstance(zone).apply { timeInMillis = instant }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Subtract successfully fetched intervals; readings themselves cannot prove coverage. */
fun uncoveredHistory(range: HistoryRange, covered: List<HistoryRange>): List<HistoryRange> {
    var cursor = range.startMillis
    val missing = mutableListOf<HistoryRange>()
    for (interval in covered.sortedBy { it.startMillis }) {
        if (interval.endExclusiveMillis <= cursor) continue
        if (interval.startMillis >= range.endExclusiveMillis) break
        if (interval.startMillis > cursor) missing += HistoryRange(cursor, interval.startMillis)
        cursor = maxOf(cursor, interval.endExclusiveMillis).coerceAtMost(range.endExclusiveMillis)
    }
    if (cursor < range.endExclusiveMillis) missing += HistoryRange(cursor, range.endExclusiveMillis)
    return missing
}
