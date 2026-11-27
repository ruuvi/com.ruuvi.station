package com.ruuvi.station.history

import com.google.gson.Gson
import java.io.File
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HistoryArchiveTest {
    @get:Rule val folder = TemporaryFolder()
    private val sensor = "FA:00:00:00:00:01"
    private val other = "FA:00:00:00:00:02"
    private val scope = HistoryPageCache.hash("synthetic-session")
    private val local: HistoryLocalLoad = { _, _ -> HistoryLocalPage(emptyList(), null) }
    private val decode: HistoryDecode = { mapOf("t" to 20.0) }
    private fun page(sensor: String, times: List<Long>) = HistoryPage(sensor, times.map { HistoryReading(it, "00", null) })

    @Test fun `three year A B A navigation and app restart reuse archive then request only new tail`() = runBlocking {
        val root = folder.newFolder()
        var now = 1_800_000_000L
        val duration = 1096 * 86400L
        val origin = now - duration
        val requests = mutableListOf<HistoryQuery>()
        var archive = HistoryPageCache(root)
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var controller = HistoryController(owner) { now * 1000 }
        fun stream() = HistoryStream(archive, { query ->
            synchronized(requests) { requests += query }; delay(2)
            val last = origin + (query.window.until - origin) / 900 * 900
            page(query.sensor, generateSequence(last) { it - 900 }.takeWhile { it >= query.window.since }.take(1500).toList())
        }, { now * 1000 })
        suspend fun open(id: String) {
            val source = stream()
            val window = HistoryWindow(now - duration, now)
            controller.start(id, id, window) { bounds, refresh -> source.snapshots(id, bounds, listOf("t"), scope, "v1", refresh, local, decode) }
            withTimeout(30000) { controller.state.first { it.sensor == id && it.overview?.complete == true } }
            assertEquals(duration / 900 + 1, controller.state.value.overview!!.readings)
        }
        try {
            open(sensor); controller.stop(); open(other); controller.stop()
            synchronized(requests) { requests.clear() }
            val began = System.nanoTime()
            open(sensor)
            assertTrue(requests.isEmpty()); assertNull(controller.state.value.progress)
            println("Synthetic three-year A/B/A archive reopen: ${(System.nanoTime() - began) / 1_000_000}ms; readings=${duration / 900 + 1}; HTTP=0")
            controller.stop()
            archive = HistoryPageCache(root); controller = HistoryController(owner) { now * 1000 }
            open(sensor); assertTrue(requests.isEmpty()); controller.stop()
            val synced = now; now += 2 * 86400 // dense readings have now aged into the sparse lane
            open(sensor)
            assertTrue(requests.isNotEmpty()); assertTrue(requests.all { it.window.since > synced })
            controller.stop(); requests.clear(); open(sensor); assertTrue(requests.isEmpty())
            var exported = 0L
            stream().read(sensor, HistoryWindow(now - duration, now), scope, HistoryRefresh.opening(now * 1000)) { page, _ -> exported += page.readings.size }
            assertEquals(duration / 900 + 1, exported); assertTrue(requests.isEmpty())
        } finally { controller.stop(); owner.cancel() }
    }

    @Test fun `cache-only preparation never says downloading and a missing file repairs only its range`() = runBlocking {
        val root = folder.newFolder(); val now = 2_000_000L
        val window = HistoryWindow(100, 6100)
        val requests = mutableListOf<HistoryQuery>()
        fun stream(cache: HistoryPageCache) = HistoryStream(cache, { q ->
            requests += q
            page(q.sensor, (q.window.until downTo q.window.since).take(1500))
        }, { now * 1000 })
        val archive = HistoryPageCache(root)
        stream(archive).snapshots(sensor, window, listOf("t"), scope, "v", HistoryRefresh.opening(now * 1000), local, decode).last()
        requests.clear()
        val warm = stream(HistoryPageCache(root)).snapshots(sensor, window, listOf("t"), scope, "v", HistoryRefresh.opening(now * 1000 + 1), local, decode).toList()
        assertTrue(requests.isEmpty()); assertTrue(warm.none { it.downloading }); assertEquals(6001L, warm.last().plot.readings)
        val entry = archive.candidates(scope, sensor, now * 1000).first { it.lower > window.since && it.upper < window.until }
        root.walkTopDown().single { it.name == "${entry.id}.json" }.delete()
        val repaired = stream(HistoryPageCache(root)).snapshots(sensor, window, listOf("t"), scope, "v", HistoryRefresh.opening(now * 1000), local, decode).last()
        assertEquals(6001L, repaired.plot.readings)
        assertTrue(requests.isNotEmpty()); assertTrue(requests.all { it.window.intersect(entry.window) == it.window })
    }

    @Test fun `minute pages compact and newer overlap replaces rows without erasing adjacent history`() = runBlocking {
        val root = folder.newFolder(); val archive = HistoryPageCache(root)
        repeat(100) { index ->
            val window = HistoryWindow(index * 60L, index * 60L + 59)
            archive.save(page(sensor, listOf(window.until)), HistoryQuery(sensor, window, HistoryMode.DENSE), window, scope, index.toLong())
        }
        val entries = HistoryPageCache(root).candidates(scope, sensor, 1000)
        assertEquals(1, entries.size); assertEquals(HistoryWindow(0, 5999), entries.single().window)
        assertEquals(100, archive.read(entries.single(), sensor)!!.page.readings.size)
        val replacement = HistoryWindow(60, 119)
        archive.save(page(sensor, emptyList()), HistoryQuery(sensor, replacement, HistoryMode.DENSE), replacement, scope, 1000)
        val merged = archive.candidates(scope, sensor, 1000).single()
        assertEquals(99, archive.read(merged, sensor)!!.page.readings.size)
        assertFalse(archive.read(merged, sensor)!!.page.readings.any { it.timestamp in replacement })
    }

    @Test fun `legacy cache migration preserves raw values coverage and old files`() = runBlocking {
        val legacy = folder.newFolder(); val destination = folder.newFolder()
        val cached = HistoryPageCache.Cached(page(sensor, listOf(200, 100)), "v", listOf(mapOf("t" to 20.0), mapOf("t" to 10.0)))
        val bytes = Gson().toJson(cached).toByteArray()
        val entry = HistoryPageCache.Entry(UUID.randomUUID().toString(), scope, HistoryPageCache.hash(sensor), HistoryMode.SPARSE, 100, 200, 1000, bytes.size.toLong())
        File(legacy, "${entry.id}.json").writeBytes(bytes)
        File(legacy, "index.json").writeText(Gson().toJson(listOf(entry)))
        repeat(2) {
            val archive = HistoryPageCache(destination, legacyRoot = legacy)
            val imported = archive.candidates(scope, sensor, 1000).single()
            assertEquals(entry, imported); assertEquals(cached, archive.read(imported, sensor))
        }
        assertTrue(File(legacy, "${entry.id}.json").exists())
    }

    @Test fun `export does not redownload adjacent archive pages compacted by its own new tail`() = runBlocking {
        val archive = HistoryPageCache(folder.newFolder()); val stored = HistoryWindow(10, 50)
        archive.save(page(sensor, (50L downTo 10).toList()), HistoryQuery(sensor, stored, HistoryMode.SPARSE), stored, scope, 0)
        val requests = mutableListOf<HistoryQuery>(); val exported = mutableListOf<Long>()
        val stream = HistoryStream(archive, { q -> requests += q; page(sensor, (q.window.until downTo q.window.since).toList()) }, { 2_000_000_000L })
        stream.read(sensor, HistoryWindow(0, 100), scope, HistoryRefresh.opening(2_000_000_000L)) { page, _ -> exported += page.readings.map { it.timestamp } }
        assertEquals((100L downTo 0).toList(), exported)
        assertEquals(listOf(HistoryWindow(51, 100), HistoryWindow(0, 9)), requests.map { it.window })
    }

    @Test fun `damaged index never deletes stored page files`() = runBlocking {
        val root = folder.newFolder(); val archive = HistoryPageCache(root); val window = HistoryWindow(0, 100)
        archive.save(page(sensor, listOf(100, 0)), HistoryQuery(sensor, window, HistoryMode.SPARSE), window, scope, 0)
        val entry = archive.candidates(scope, sensor, 100).single()
        val path = root.walkTopDown().single { it.name == "${entry.id}.json" }
        File(path.parentFile, "index.json").writeText("broken")
        try { HistoryPageCache(root).candidates(scope, sensor, 100); fail("A damaged index must report failure") } catch (_: Exception) { }
        assertTrue(path.isFile)
    }
    @Test fun `many sensors keep independent archives even after metadata leaves memory`() = runBlocking {
        val root = folder.newFolder(); val archive = HistoryPageCache(root, 230)
        val sensors = (1..12).map { "FA:00:00:00:00:%02X".format(it) }
        for (id in sensors) {
            val window = HistoryWindow(1, 1)
            archive.save(page(id, listOf(1)), HistoryQuery(id, window, HistoryMode.DENSE), window, scope, 0)
        }
        for (id in sensors) {
            val entries = archive.candidates(scope, id, 100)
            assertEquals(1, entries.size)
            assertEquals(listOf(1L), archive.read(entries.single(), id)!!.page.readings.map { it.timestamp })
        }
    }

    @Test fun `compaction cannot let another older overlap hide a newly replaced reading`() = runBlocking {
        val archive = HistoryPageCache(folder.newFolder())
        suspend fun save(window: HistoryWindow, times: List<Long>, time: Long, payload: String) {
            archive.save(HistoryPage(sensor, times.sortedDescending().map { HistoryReading(it, payload, null) }),
                HistoryQuery(sensor, window, HistoryMode.SPARSE), window, scope, time)
        }
        save(HistoryWindow(0, 2999), (0L..2998 step 2).toList(), 1, "00")
        save(HistoryWindow(1000, 3999), (1001L..3999 step 2).toList(), 2, "00")
        save(HistoryWindow(1400, 1499), (1400L..1498 step 2).toList(), 3, "64")
        val stream = HistoryStream(archive, { error("Stored coverage must be reused") }, { 2_000_000_000L })
        val result = stream.snapshots(sensor, HistoryWindow(0, 3999), listOf("t"), scope, "raw", HistoryRefresh.opening(2_000_000_000L), local) {
            mapOf("t" to it.data.toInt(16).toDouble())
        }.last().plot
        assertEquals(2000L, result.readings)
        assertEquals(100.0, result.statistics.getValue("t").maximum, 0.0)
    }

}
