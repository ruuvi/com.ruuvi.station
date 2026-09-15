package com.ruuvi.station.history

import com.ruuvi.station.graph.cloudSpinnerVisible
import com.ruuvi.station.network.domain.HistorySyncState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewportTest {
    @Test fun `viewport changes debounce and gesture end flushes immediately`() = runTest {
        val controller = HistoryViewportController()
        val seen = mutableListOf<HistoryViewport>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { controller.updates.toList(seen) }
        runCurrent()
        val selection = HistorySelection.Rolling()
        controller.update("A", selection, HistoryRange(0, 100), false)
        runCurrent(); advanceTimeBy(99); runCurrent()
        assertEquals(1, seen.size)
        controller.update("A", selection, HistoryRange(10, 110), false)
        runCurrent(); advanceTimeBy(100); runCurrent()
        assertEquals(HistoryRange(10, 110), seen.last().range)
        controller.update("A", selection, HistoryRange(20, 120), false)
        runCurrent(); advanceTimeBy(10)
        controller.update("A", selection, HistoryRange(20, 120), true)
        controller.update("A", selection, HistoryRange(20, 120), false)
        runCurrent()
        assertEquals(HistoryRange(20, 120), seen.last().range)
        assertTrue(seen.last().finished)
    }

    @Test fun `a newer viewport cancels stale processing`() = runTest {
        val controller = HistoryViewportController()
        val finished = mutableListOf<HistoryRange?>()
        var cancelled = 0
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.updates.filter { it.sensorId != null }.collectLatest {
                try { delay(1000); finished.add(it.range) }
                catch (e: CancellationException) { cancelled++; throw e }
            }
        }
        controller.update("A", HistorySelection.Rolling(), HistoryRange(0, 10), true)
        runCurrent(); advanceTimeBy(100)
        controller.update("A", HistorySelection.Rolling(), HistoryRange(10, 20), true)
        runCurrent(); advanceTimeBy(1000); runCurrent()
        assertEquals(1, cancelled)
        assertEquals(listOf(HistoryRange(10, 20)), finished)
    }

    @Test fun `one spinner combines account and active history loading only`() {
        val loading = HistorySyncState(sensorId = "A", loading = true)
        assertTrue(cloudSpinnerVisible(false, "A", loading))
        assertTrue(cloudSpinnerVisible(true, "A", loading))
        assertFalse(cloudSpinnerVisible(false, "B", loading))
        assertFalse(cloudSpinnerVisible(false, null, loading))
        assertFalse(cloudSpinnerVisible(false, "A", loading.copy(loading = false, failed = true)))
        assertTrue(cloudSpinnerVisible(true, "A", loading.copy(loading = false)))
    }
}
