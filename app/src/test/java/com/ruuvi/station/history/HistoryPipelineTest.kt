package com.ruuvi.station.history

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryPipelineTest {
    @get:Rule val folder = TemporaryFolder()
    private val sensor = "FA:00:00:00:00:01"
    private val query = HistoryQuery(sensor, HistoryWindow(1, 10), HistoryMode.SPARSE)
    private fun page(q: HistoryQuery) = HistoryPage(sensor, listOf(HistoryReading(q.window.until, "00", null)))

    @Test fun `ordinary retries do not pace successful pages but throttling does`() = runTest {
        for (status in listOf(500, 503, 429)) {
            val waits = mutableListOf<Long>(); var calls = 0
            val loader = HistoryPageLoader({ q -> if (++calls == 1) throw HistoryFailure.Http(status); page(q) }, { waits += it }, { 1.0 })
            loader.next(query); loader.next(query)
            assertEquals(if (status == 429) listOf(2000L, 2000L) else listOf(2000L), waits)
        }
    }

    @Test fun `one page prefetch overlaps processing without concurrent HTTP or unbounded buffering`() = runTest {
        var calls = 0; var active = 0; var maximumActive = 0; var consumed = 0
        val loader = HistoryPageLoader({ q ->
            active++; maximumActive = maxOf(maximumActive, active); calls++
            assertTrue(calls - consumed <= 2)
            delay(100); active--; page(q)
        })
        val began = currentTime
        loader.pages(query) { _, _, _ -> delay(100); consumed++ }
        val elapsed = currentTime - began
        assertEquals(10, consumed); assertEquals(1, maximumActive)
        assertEquals(1100L, elapsed) // Serial baseline: 10 * (100 ms HTTP + 100 ms processing) = 2000 ms.
        println("Synthetic pipeline: serial=2000ms; prefetched=${elapsed}ms; pages=$consumed; maxHTTP=$maximumActive")
    }

    @Test fun `cancellation cancels the one prefetched request`() = runTest {
        var calls = 0; var cancelled = false
        val loader = HistoryPageLoader({ q ->
            if (++calls == 2) try { awaitCancellation() } finally { cancelled = true }
            page(q)
        })
        val work = launch { loader.pages(query) { _, _, _ -> awaitCancellation() } }
        runCurrent(); assertEquals(2, calls)
        work.cancelAndJoin(); assertTrue(cancelled); assertEquals(2, calls)
    }

    @Test fun `failed prefetch still consumes preceding valid page`() = runTest {
        var calls = 0; var consumed = 0
        val loader = HistoryPageLoader({ q -> if (++calls == 2) throw HistoryFailure.Invalid(); page(q) })
        try { loader.pages(query) { _, _, _ -> delay(50); consumed++ }; fail() } catch (_: HistoryFailure.Invalid) { }
        assertEquals(1, consumed)
    }

    @Test fun `statistics retain while moving then expose raw partial ready and failed states`() {
        val window = HistoryWindow(0, 100); val zoom = HistoryWindow(40, 60)
        val old = HistoryStatistics(1.0, 9.0, 4.0); val fresh = HistoryStatistics(3.0, 7.0, 5.0)
        val overview = HistoryPlot(window, statistics = mapOf("t" to old), complete = true)
        val moving = HistoryController.State(sensor, overview, visible = zoom, moving = true, retainedStatistics = mapOf("t" to old))
        assertEquals(HistoryStatisticsDisplay(old, HistoryStatisticsPhase.UPDATING), moving.statisticsDisplay("t"))
        val partial = moving.copy(moving = false, detail = HistoryPlot(zoom, statistics = mapOf("t" to fresh)))
        assertEquals(HistoryStatisticsDisplay(fresh, HistoryStatisticsPhase.PARTIAL), partial.statisticsDisplay("t"))
        val cached = partial.copy(detail = partial.detail!!.copy(coverage = listOf(zoom)))
        assertEquals(HistoryStatisticsPhase.READY, cached.statisticsDisplay("t").phase)
        assertEquals(HistoryStatisticsPhase.FAILED, partial.copy(error = 1).statisticsDisplay("t").phase)
        val empty = partial.copy(detail = HistoryPlot(zoom, complete = true))
        assertNull(empty.statisticsDisplay("t").values)
        assertNull(HistoryController.State(sensor = "another").statisticsDisplay("t").values)
    }

    @Test fun `automatic refresh keeps complete graph and progress stable`() = runTest {
        var clock = 2_000_000_000L; var calls = 0
        val controller = HistoryController(this) { clock }
        // Use a non-negative two-year window.
        controller.start(sensor, "rolling", HistoryWindow(0, clock / 1000)) { bounds, _ -> flow {
            calls++
            emit(HistoryProgress(HistoryPlot(bounds), 0f, 0, 0))
            delay(200)
            emit(HistoryProgress(HistoryPlot(bounds, complete = true), 1f, 1, 0))
        } }
        advanceTimeBy(201); runCurrent(); assertEquals(1, calls)
        val before = controller.state.value.overview
        clock += 60000
        advanceTimeBy(60000); runCurrent()
        assertEquals(2, calls); assertNull(controller.state.value.progress)
        assertEquals(before, controller.state.value.overview)
        advanceTimeBy(201); runCurrent(); assertTrue(controller.state.value.overview!!.complete)
        assertNull(controller.state.value.progress); controller.stop()
    }

    @Test fun `incremental refresh after cache eviction never reloads archive and preserves old plot`() = runBlocking {
        var now = 100_000_000L
        val queries = mutableListOf<HistoryQuery>()
        val cache = HistoryPageCache(folder.newFolder(), 0)
        val stream = HistoryStream(cache, { q ->
            queries += q
            HistoryPage(sensor, listOf(q.window.until, q.window.since).distinct().map { HistoryReading(it, "00", null) })
        }, { now * 1000 })
        val scope = HistoryPageCache.hash("evicted-live")
        val local: HistoryLocalLoad = { _, _ -> HistoryLocalPage(emptyList(), null) }
        val start = now - 730 * 86400L
        var previous = stream.snapshots(sensor, HistoryWindow(start, now), listOf("t"), scope, "v", null, local) { mapOf("t" to 20.0) }.last().plot
        repeat(3) {
            val synced = now; queries.clear(); now += 61
            val policy = HistoryRefresh(synced + 1, now * 1000, synced + 1)
            val next = stream.snapshots(sensor, HistoryWindow(start + now - synced, now), listOf("t"), scope, "v", policy, local) { mapOf("t" to 21.0) }.last().plot
            assertTrue(queries.isNotEmpty()); assertTrue(queries.all { it.window.since > synced })
            val merged = next.preservingLoadedHistory(previous)
            assertTrue(merged.samples.getValue("t").any { it.timestamp <= synced })
            assertTrue(merged.samples.getValue("t").any { it.timestamp > synced })
            assertTrue(merged.samples.getValue("t").size <= 400)
            assertTrue(merged.statisticsPartial)
            val chart = com.ruuvi.station.graph.HistoryChartData(
                HistoryController.State(sensor, merged, visible = merged.window), "t", emptyList(), {}, { _, _ -> })
            assertEquals(20.0 to 21.0, chart.bounds())
            previous = merged
        }
    }

    @Test fun `minute refresh reuses archive and queries only recent tail or moving mode boundary`() = runBlocking {
        var now = 2_000_000L
        val queries = mutableListOf<HistoryQuery>()
        val cache = HistoryPageCache(folder.newFolder())
        val stream = HistoryStream(cache, { q -> queries += q; HistoryPage(sensor, emptyList()) }, { now * 1000 })
        val scope = HistoryPageCache.hash("synthetic")
        val local: HistoryLocalLoad = { _, _ -> HistoryLocalPage(emptyList(), null) }
        stream.snapshots(sensor, HistoryWindow(1, now), listOf("t"), scope, "v", null, local) { emptyMap() }.last()
        repeat(3) {
            queries.clear(); now += 60
            stream.snapshots(sensor, HistoryWindow(1, now), listOf("t"), scope, "v", HistoryRefresh.live(now * 1000), local) { emptyMap() }.last()
            assertTrue(queries.isNotEmpty())
            assertTrue(queries.all { it.window.seconds <= 61 && it.window.since >= now - 86460 })
        }
    }
}
