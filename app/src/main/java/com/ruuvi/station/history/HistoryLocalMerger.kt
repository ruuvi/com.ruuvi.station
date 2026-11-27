package com.ruuvi.station.history

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class HistoryLocalPage<T>(val rows: List<T>, val next: String?)

/** Descending keyset merge. Cloud wins same-second ties; local-only seconds remain. */
object HistoryLocalMerger {
    suspend fun <T> merge(cloud: List<T>, window: HistoryWindow, timestamp: (T) -> Long,
                         local: suspend (HistoryWindow, String?) -> HistoryLocalPage<T>,
                         consume: suspend (List<T>) -> Unit) {
        val ordered = cloud.sortedByDescending(timestamp)
        var ci = 0; var li = 0
        var cursor: String? = null
        var page = local(window, null)
        var previousLocal = window.until
        var last: Long? = null
        var buffer = ArrayList<T>(HistoryQuery.PAGE_SIZE)
        while (true) {
            currentCoroutineContext().ensureActive()
            if (page.rows.size > HistoryQuery.PAGE_SIZE || page.next != null && page.rows.isEmpty()) throw HistoryFailure.Invalid()
            if (li == page.rows.size && page.next != null) {
                if (page.next == cursor) throw HistoryFailure.Invalid()
                cursor = page.next; page = local(window, cursor); li = 0; continue
            }
            val row = if (li < page.rows.size) {
                val localRow = page.rows[li]
                val time = timestamp(localRow)
                if (time !in window || time > previousLocal) throw HistoryFailure.Invalid()
                if (ci < ordered.size && timestamp(ordered[ci]) >= time) ordered[ci++]
                else { li++; previousLocal = time; localRow }
            } else if (ci < ordered.size) ordered[ci++] else break
            if (last == timestamp(row)) continue
            last = timestamp(row); buffer += row
            if (buffer.size == HistoryQuery.PAGE_SIZE) { consume(buffer); buffer = ArrayList(HistoryQuery.PAGE_SIZE) }
        }
        if (buffer.isNotEmpty()) consume(buffer)
    }
}
