package com.ruuvi.station.history

import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class HistoryPreviewTest {
    @get:Rule val folder = TemporaryFolder()
    private val sensor = "FA:00:00:00:00:01"
    private val other = "FA:00:00:00:00:02"
    private val scope = HistoryPageCache.hash("synthetic-preview")
    private val decode: HistoryDecode = { mapOf("t" to (it.timestamp % 50).toDouble()) }

    @Test fun `ten day and three year revisits draw synchronously and cold reopen draws before raw replay`() = runBlocking {
        for (days in listOf(10, 1096)) {
            val root = folder.newFolder(); var archive = HistoryPageCache(root)
            val now = 1_800_000_000L; val window = HistoryWindow(now - days * 86400L, now)
            val spacing = if (days == 10) 60L else 900L
            val requests = mutableListOf<HistoryQuery>()
            var gate: CompletableDeferred<Unit>? = null
            var entered = CompletableDeferred<Unit>()
            val local: HistoryLocalLoad = { _, _ ->
                gate?.let { entered.complete(Unit); it.await() }; HistoryLocalPage(emptyList(), null)
            }
            fun source(id: String, bounds: HistoryWindow, refresh: HistoryRefresh) = HistoryStream(archive, { q ->
                synchronized(requests) { requests += q }
                val last = window.since + (q.window.until - window.since) / spacing * spacing
                HistoryPage(q.sensor, generateSequence(last) { it - spacing }.takeWhile { it >= q.window.since }.take(1500)
                    .map { HistoryReading(it, "00", null) }.toList())
            }, { now * 1000 }).snapshots(id, bounds, listOf("t"), scope, "v1", refresh, local, decode, "key:$id:$days")
            val owner = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            var controller = HistoryController(owner, archive.previews) { now * 1000 }
            fun start(id: String) = controller.start(id, "key:$id:$days", window, previewScope = scope) { bounds, refresh -> source(id, bounds, refresh) }
            suspend fun complete() = withTimeout(30000) { controller.state.first { it.overview?.complete == true } }
            try {
                start(sensor); val expected = complete().overview!!
                assertEquals(days * 86400L / spacing + 1, expected.readings)
                start(other); complete(); requests.clear()
                gate = CompletableDeferred(); entered = CompletableDeferred()
                val warmStart = System.nanoTime(); start(sensor)
                val warmMillis = (System.nanoTime() - warmStart) / 1_000_000.0
                assertEquals(expected.samples, controller.state.value.overview!!.samples)
                assertEquals(expected.statistics, controller.state.value.statisticsDisplay("t").values?.let { mapOf("t" to it) })
                assertNotEquals(HistoryStatisticsPhase.READY, controller.state.value.statisticsDisplay("t").phase)
                assertNull(controller.state.value.progress)
                withTimeout(5000) { entered.await() }; delay(300)
                assertEquals(expected.samples, controller.state.value.overview!!.samples)
                assertTrue(requests.isEmpty())
                gate!!.complete(Unit); complete(); controller.stop(clear = true)
                archive = HistoryPageCache(root); controller = HistoryController(owner, archive.previews) { now * 1000 }
                gate = CompletableDeferred(); entered = CompletableDeferred()
                val coldStart = System.nanoTime(); start(sensor)
                withTimeout(5000) { controller.state.first { it.overview?.samples?.get("t")?.isNotEmpty() == true } }
                val coldMillis = (System.nanoTime() - coldStart) / 1_000_000.0
                assertEquals(expected.samples, controller.state.value.overview!!.samples)
                assertFalse(controller.state.value.overview!!.complete)
                assertFalse(gate!!.isCompleted); assertTrue(requests.isEmpty())
                gate!!.complete(Unit); complete()
                println("Synthetic $days-day first plotted state: memory=${warmMillis}ms disk=${coldMillis}ms; readings=${expected.readings}; no raw replay required")
            } finally { gate?.complete(Unit); controller.stop(); owner.cancel() }
        }
    }

    private fun plot(window: HistoryWindow = HistoryWindow(100, 200)) = HistoryPlot(window,
        samples = mapOf("t" to listOf(HistorySample(window.since, 10.0, 0), HistorySample(window.until, 20.0, 0))),
        statistics = mapOf("t" to HistoryStatistics(10.0, 20.0, 15.0)), coverage = listOf(window), complete = true, readings = 2)

    @Test fun `previews isolate sensor account server calibration duration and clip rolling boundaries`() {
        val root = folder.newFolder(); val store = HistoryPreviewStore(root)
        store.save(scope, sensor, "v1:10days", plot())
        assertNull(store.peek("another-session", sensor, "v1:10days", HistoryWindow(100, 200)))
        assertNull(store.peek(scope, other, "v1:10days", HistoryWindow(100, 200)))
        assertNull(store.peek(scope, sensor, "v2:10days", HistoryWindow(100, 200)))
        assertNull(store.peek(scope, sensor, "v1:3years", HistoryWindow(100, 200)))
        assertNull(store.peek(scope, sensor, "v1:10days", HistoryWindow(0, 99)))
        val shifted = store.peek(scope, sensor, "v1:10days", HistoryWindow(150, 250))!!
        assertEquals(listOf(200L), shifted.samples.getValue("t").map { it.timestamp })
        assertTrue(shifted.statisticsPartial); assertFalse(shifted.complete)
        assertEquals(listOf(HistoryWindow(150, 200)), shifted.coverage)
        store.prune(sensor, 150)
        val pruned = HistoryPreviewStore(root).read(scope, sensor, "v1:10days", HistoryWindow(150, 250))!!
        assertEquals(listOf(200L), pruned.samples.getValue("t").map { it.timestamp })
        assertTrue(pruned.statistics.isEmpty())
        store.prune(sensor, 201)
        assertNull(store.peek(scope, sensor, "v1:10days", HistoryWindow(100, 200)))
        store.save(scope, sensor, "v1", plot()); store.remove(scope, sensor)
        assertNull(HistoryPreviewStore(root).read(scope, sensor, "v1", HistoryWindow(100, 200)))
    }

    @Test fun `preview corruption falls back and memory disk limits never evict raw files`() {
        val root = folder.newFolder(); val raw = File(root, "raw-history.bin").apply { writeText("untouched") }
        val store = HistoryPreviewStore(root)
        repeat(66) { store.save(scope, sensor, "key:$it", plot()) }
        assertNull(store.peek(scope, sensor, "key:0", HistoryWindow(100, 200)))
        assertTrue(root.listFiles()!!.count { it.extension == "json" } <= 64)
        val loaded = HistoryPreviewStore(root)
        assertNotNull(loaded.read(scope, sensor, "key:65", HistoryWindow(100, 200)))
        root.listFiles()!!.filter { it.extension == "json" }.forEach { it.writeText("broken") }
        assertNull(HistoryPreviewStore(root).read(scope, sensor, "key:65", HistoryWindow(100, 200)))
        assertEquals("untouched", raw.readText())
    }
    @Test fun `known allowance opens locally without waiting for server or pruning`() = runBlocking {
        val context = io.mockk.mockk<android.content.Context>()
        io.mockk.every { context.noBackupFilesDir } returns folder.newFolder()
        io.mockk.every { context.cacheDir } returns folder.newFolder()
        val tokens = io.mockk.mockk<com.ruuvi.station.network.domain.NetworkTokenRepository>()
        io.mockk.every { tokens.getTokenInfo() } returns com.ruuvi.station.network.data.NetworkTokenInfo("synthetic@example.invalid", "synthetic-token")
        val preferences = io.mockk.mockk<com.ruuvi.station.app.preferences.PreferencesRepository>()
        io.mockk.every { preferences.isDevServerEnabled() } returns false
        val settings = io.mockk.mockk<com.ruuvi.station.database.domain.SensorSettingsRepository>()
        val configuration = com.ruuvi.station.database.tables.SensorSettings().apply {
            id = sensor; networkSensor = true; maxHistoryDays = 1096
        }
        io.mockk.every { settings.getSensorSettings(sensor) } returns configuration
        val local = io.mockk.mockk<com.ruuvi.station.database.domain.SensorHistoryRepository>()
        val flags = io.mockk.mockk<com.ruuvi.station.feature.domain.RuntimeBehavior>()
        io.mockk.every { flags.isFeatureEnabled(any()) } returns true
        val transport = io.mockk.mockk<HistoryTransport>()
        val repository = ProgressiveHistoryRepository(context, tokens, preferences, settings, local, flags, transport)
        assertEquals(1096, repository.access(sensor, cachedFirst = true).days)
        io.mockk.coVerify(exactly = 0) { transport.allowance(any(), any(), any()) }
        io.mockk.verify(exactly = 0) { local.schedulePruning() }
    }

}
