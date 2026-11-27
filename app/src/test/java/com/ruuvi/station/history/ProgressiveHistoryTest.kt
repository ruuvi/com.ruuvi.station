package com.ruuvi.station.history

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressiveHistoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val sensor = "FA:00:00:00:00:01"
    private val scope = HistoryPageCache.hash("synthetic-account-server-session")
    private val now = 2_000_000L
    private val emptyLocal: HistoryLocalLoad = { _, _ -> HistoryLocalPage(emptyList(), null) }
    private val decode: HistoryDecode = { mapOf("t" to it.data.toInt(16).toDouble()) }
    private fun row(time: Long, value: Int = (time % 200).toInt()) = HistoryReading(time, "%02x".format(value), null)
    private fun cache() = HistoryPageCache(folder.newFolder())

    @Test fun `short pages continue to empty and explicit modes split at 24 hours`() = runBlocking {
        val queries = mutableListOf<HistoryQuery>()
        val rows = listOf(row(now - 2), row(now - 86401), row(now - 100000))
        val window = HistoryWindow(now - 200000, now)
        val stream = HistoryStream(cache(), { query ->
            queries += query
            HistoryPage(sensor, rows.filter { it.timestamp in query.window }.take(1))
        }, { now * 1000 })
        val updates = stream.snapshots(sensor, window, listOf("t"), scope, "v1", null, emptyLocal, decode).toList()
        val end = updates.last()
        assertTrue(end.plot.complete); assertEquals(3L, end.plot.readings); assertEquals(1f, end.fraction)
        assertEquals(5, queries.size)
        assertEquals(listOf(HistoryMode.DENSE, HistoryMode.DENSE, HistoryMode.SPARSE, HistoryMode.SPARSE, HistoryMode.SPARSE), queries.map { it.mode })
        assertEquals(now - 3, queries[1].window.until)
        assertEquals(listOf(window), end.plot.coverage)
    }

    @Test fun `warm archive needs no requests and export uses all raw readings`() = runBlocking {
        val cache = cache(); val window = HistoryWindow(10000, 15000)
        val rows = (10000L..15000L).map { row(it) }.asReversed()
        var requests = 0
        val stream = HistoryStream(cache, { q -> requests++; HistoryPage(sensor, rows.filter { it.timestamp in q.window }.take(1500)) }, { now * 1000 })
        val initial = stream.snapshots(sensor, window, listOf("t"), scope, "v1", null, emptyLocal, decode).last()
        assertEquals(5001L, initial.plot.readings); assertTrue(initial.plot.samples.getValue("t").size <= 400)
        val firstRequests = requests
        val repeated = stream.snapshots(sensor, window, listOf("t"), scope, "v1", HistoryRefresh.opening(now * 1000 + 1), emptyLocal, decode).last()
        assertEquals(firstRequests, requests); assertEquals(initial.plot.samples, repeated.plot.samples)
        val exported = mutableListOf<Long>()
        stream.read(sensor, window, scope, HistoryRefresh.opening(now * 1000 + 1)) { page, _ -> exported += page.readings.map { it.timestamp } }
        assertEquals(rows.map { it.timestamp }, exported); assertEquals(firstRequests, requests)
    }

    @Test fun `reopening reuses recent history and changed calibration only redecodes local pages`() = runBlocking {
        val cache = cache(); val window = HistoryWindow(now - 300000, now)
        val queries = mutableListOf<HistoryQuery>(); var clock = now * 1000
        val rows = listOf(row(now - 10), row(now - 90000), row(now - 250000))
        val stream = HistoryStream(cache, { q -> queries += q; HistoryPage(sensor, rows.filter { it.timestamp in q.window }) }, { clock })
        stream.snapshots(sensor, window, listOf("t"), scope, "v1", null, emptyLocal, decode).last()
        queries.clear(); clock++
        val result = stream.snapshots(sensor, window, listOf("t"), scope, "v2", HistoryRefresh.opening(clock), emptyLocal) { mapOf("t" to 1000.0) }.last()
        assertTrue(queries.isEmpty())
        assertEquals(1000.0, result.plot.statistics.getValue("t").average, 0.0)
    }

    @Test fun `corrupt cache loses coverage and session scope cannot reuse another account`() = runBlocking {
        val dir = folder.newFolder(); val cache = HistoryPageCache(dir)
        val query = HistoryQuery(sensor, HistoryWindow(10, 20), HistoryMode.SPARSE)
        cache.save(HistoryPage(sensor, listOf(row(20))), query, query.window, scope, 100)
        assertTrue(cache.candidates(HistoryPageCache.hash("another"), sensor, 100).isEmpty())
        val entry = cache.candidates(scope, sensor, 100).single()
        dir.walkTopDown().single { it.name == "${entry.id}.json" }.writeText("broken")
        assertNull(cache.read(entry, sensor)); assertTrue(cache.candidates(scope, sensor, 100).isEmpty())
    }

    @Test fun `cache stays bounded and survives index reopening`() = runBlocking {
        val dir = folder.newFolder(); val cache = HistoryPageCache(dir, 1000)
        repeat(30) { i ->
            val query = HistoryQuery(sensor, HistoryWindow(i.toLong(), i.toLong()), HistoryMode.SPARSE)
            cache.save(HistoryPage(sensor, listOf(row(i.toLong()))), query, query.window, scope, i.toLong())
        }
        val entries = HistoryPageCache(dir, 1000).candidates(scope, sensor, 100)
        assertTrue(entries.isNotEmpty()); assertTrue(entries.size < 30); assertTrue(entries.sumOf { it.bytes } <= 1000)
        assertEquals(29L, entries.maxOf { it.upper })
    }

    @Test fun `local keyset merge is descending deduplicated and cloud wins ties`() = runBlocking {
        data class Value(val timestamp: Long, val origin: String)
        val pages = listOf(listOf(Value(10, "local"), Value(9, "local")), listOf(Value(9, "local-duplicate"), Value(7, "local")))
        val output = mutableListOf<Value>()
        HistoryLocalMerger.merge(listOf(Value(9, "cloud"), Value(8, "cloud")), HistoryWindow(0, 10), { it.timestamp }, { _, cursor ->
            val index = cursor?.toInt() ?: 0
            HistoryLocalPage(pages[index], if (index == 0) "1" else null)
        }) { output += it }
        assertEquals(listOf(10L, 9L, 8L, 7L), output.map { it.timestamp }); assertEquals("cloud", output[1].origin)
    }

    @Test fun `raw weighted average and gap identity survive bounded sampling`() {
        val sampler = HistorySampler(HistoryWindow(0, 20000), listOf("t"))
        sampler.addPage((0L..5000L).map { HistoryValues(it, mapOf("t" to if (it == 2000L) 9999.0 else 1.0)) })
        sampler.addPage(listOf(HistoryValues(20000, mapOf("t" to -1000.0))))
        val result = sampler.plot(listOf(HistoryWindow(0, 20000)), true)
        val samples = result.samples.getValue("t")
        assertTrue(samples.size <= 400); assertTrue(samples.any { it.value == 9999.0 })
        assertEquals(20000L, samples.last().timestamp); assertNotEquals(samples.first().segment, samples.last().segment)
        assertEquals(-1000.0, result.statistics.getValue("t").minimum, 0.0)
        val expectedArea = 5000.0 + 9998.0 + 15000.0 * (1.0 - 1000.0) / 2
        assertEquals(expectedArea / 20000, result.statistics.getValue("t").average, 1e-9)
    }

    @Test fun `three years of minute data retains at most 400 actual points and raw stats`() {
        val count = 1096 * 24 * 60
        val window = HistoryWindow(0, (count - 1) * 60L)
        val sampler = HistorySampler(window, listOf("t", "p"))
        for (first in 0 until count step 1500) sampler.addPage((first until minOf(count, first + 1500)).map {
            HistoryValues(it * 60L, mapOf("t" to (it % 101).toDouble(), "p" to (1000 + it % 17).toDouble()))
        })
        val result = sampler.plot(listOf(window), true)
        assertEquals(count.toLong(), result.readings)
        assertTrue(result.samples.values.all { it.size <= 400 && it.first().timestamp == 0L && it.last().timestamp == window.until })
        assertEquals(100.0, result.statistics.getValue("t").maximum, 0.0)
        assertTrue(result.samples.values.flatten().all { it.timestamp % 60 == 0L })
    }

    @Test fun `retry is bounded honors server wait and never retries invalid or forbidden responses`() = runTest {
        val waits = mutableListOf<Long>(); var calls = 0
        val query = HistoryQuery(sensor, HistoryWindow(0, 10), HistoryMode.DENSE)
        val loader = HistoryPageLoader({ calls++; throw HistoryFailure.Http(429, 5000) }, { waits += it }, { 1.0 })
        try { loader.next(query); fail() } catch (_: HistoryFailure.Http) { }
        assertEquals(6, calls); assertEquals(5, waits.size); assertTrue(waits.all { it >= 5000 && it <= 30000 })
        assertNull(HistoryPageLoader.retryDelay(HistoryFailure.Invalid(), 0))
        assertNull(HistoryPageLoader.retryDelay(HistoryFailure.Http(403), 0))
        assertNull(HistoryPageLoader.retryDelay(HistoryFailure.Http(429, 121000), 0))
    }

    @Test fun `cancelling retry cancels the real producer`() = runTest {
        var calls = 0
        val loader = HistoryPageLoader({ calls++; throw IOException() })
        val task = launch { loader.next(HistoryQuery(sensor, HistoryWindow(0, 10), HistoryMode.DENSE)) }
        runCurrent(); assertEquals(1, calls); task.cancel(); advanceTimeBy(120000); assertEquals(1, calls)
    }

    @Test fun `completed overview survives zoom and clamped zoom out without another request`() = runTest {
        val windows = mutableListOf<HistoryWindow>(); val all = HistoryWindow(100, 10000)
        val controller = HistoryController(this) { now * 1000 }
        controller.start(sensor, "selection", all) { window, _ -> flow { windows += window; emit(HistoryProgress(HistoryPlot(window, complete = true), 1f, 0, 0)) } }
        runCurrent(); assertEquals(listOf(all), windows)
        controller.interaction(true); controller.move(300.0, 600.0); runCurrent(); assertEquals(1, windows.size)
        controller.interaction(false); advanceTimeBy(199); runCurrent(); assertEquals(1, windows.size)
        advanceTimeBy(1); runCurrent(); assertEquals(2, windows.size)
        controller.interaction(true); controller.move(99.0, 10001.0); controller.interaction(false); runCurrent()
        assertEquals(2, windows.size); assertEquals(all, controller.state.value.visible)
        controller.interaction(true); controller.move(100.0, 10000.0); controller.interaction(false); runCurrent(); assertEquals(2, windows.size)
        controller.stop()
    }

    @Test fun `range changes cancel stale work and reopening restarts refresh`() = runTest {
        var cancelled = 0; var opened = 0
        val controller = HistoryController(this) { now * 1000 }
        val source: (HistoryWindow, HistoryRefresh) -> Flow<HistoryProgress> = { window, _ -> flow {
            opened++
            try { emit(HistoryProgress(HistoryPlot(window), 0f, 0, 0)); awaitCancellation() }
            finally { cancelled++ }
        } }
        controller.start(sensor, "one", HistoryWindow(100, 200), source = source); runCurrent()
        controller.start(sensor, "two", HistoryWindow(300, 400), source = source); runCurrent()
        assertEquals(1, cancelled); assertEquals(HistoryWindow(300, 400), controller.state.value.timeline)
        controller.stop(); runCurrent(); assertEquals(2, cancelled)
        controller.start(sensor, "two", HistoryWindow(300, 400), source = source); runCurrent(); assertEquals(3, opened)
        controller.stop()
    }
}
