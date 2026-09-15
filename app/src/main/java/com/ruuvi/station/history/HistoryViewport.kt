package com.ruuvi.station.history

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce

data class HistoryViewport(
    val sensorId: String? = null,
    val selection: HistorySelection? = null,
    val range: HistoryRange? = null,
    val finished: Boolean = true
)

/** Local graph requests only. This stream is deliberately independent of cloud selection. */
class HistoryViewportController {
    private val requested = MutableStateFlow(HistoryViewport())

    @OptIn(FlowPreview::class)
    val updates = requested.debounce { if (it.finished) 0L else 100L }

    fun update(sensorId: String, selection: HistorySelection, range: HistoryRange?, finished: Boolean) {
        val previous = requested.value
        // A redraw of the same viewport must not replace a pending immediate gesture-end event.
        if (!finished && previous.sensorId == sensorId && previous.selection == selection && previous.range == range) return
        requested.value = HistoryViewport(sensorId, selection, range, finished)
    }
}
