package com.ruuvi.station.history

import android.graphics.Bitmap
import android.os.SystemClock
import org.json.JSONObject
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.export.writeHistoryCsvRow
import java.util.Date
import kotlinx.coroutines.flow.last
import android.view.InputDevice
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.graph.*
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class HistoryGraphJourneyTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val sensor = "FA:00:00:00:00:01"

    @Test fun threeYearProgressiveGraphZoomPanAndReturnToOverview() {
        // Explicit opt-in. The private file is supplied out of band, never bundled or logged.
        val live = InstrumentationRegistry.getArguments().getString("liveHistory") == "true"
        val config = if (live) JSONObject(File(compose.activity.filesDir, "history-live-private.json").readText()).also {
            require(it.getBoolean("synthetic") && it.getString("base") == "https://testnet.ruuvi.com")
        } else null
        val sensor = config?.getString("sensor") ?: sensor
        val now = System.currentTimeMillis() / 1000
        val until = if (live) now else now - 7 * 86400
        // Keep the fixture comparison inside the moving retention cutoff for the entire paginated run.
        val window = HistoryWindow(until - 1096 * 86400 + if (live) 3600 else 0, until)
        val split = now - 86400
        val expected = config?.let { fixture ->
            val timestamps = mutableSetOf<Long>()
            for (timestamp in fixture.getLong("seedStart")..fixture.getLong("seedEnd") step 900L)
                if (timestamp in window && timestamp <= split) timestamps += timestamp
            for (timestamp in fixture.getLong("denseEnd") - 86400..fixture.getLong("denseEnd") step 60L)
                if (timestamp in window && timestamp > split) timestamps += timestamp
            timestamps
        }
        val count = expected?.size ?: (1096 * 1440 + 1)
        val cacheFolder = File(compose.activity.cacheDir, "history-instrumented-synthetic").apply { deleteRecursively(); mkdirs() }
        val requests = AtomicInteger()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        val controller = HistoryController(scope)
        val transport = HistoryTransport()
        val observed = mutableSetOf<Long>()
        var networkMillis = 0L
        val decode: HistoryDecode = if (live) { row ->
            val reading = TagSensorReading(BluetoothLibrary.decode(sensor, row.data, row.rssi ?: 0), Date(row.timestamp * 1000))
            mapOf("t" to requireNotNull(reading.temperature))
        } else { row -> mapOf("t" to (row.data.toInt(16) / 10.0 + 10)) }
        val stream = HistoryStream(HistoryPageCache(cacheFolder), { query ->
            val pageNumber = requests.incrementAndGet()
            if (pageNumber % (if (live) 10 else 200) == 0) println("Synthetic history pages: $pageNumber")
            if (config != null) {
                val beganRequest = SystemClock.elapsedRealtime()
                transport.load(query, config.getString("base") + "/", config.getString("token")).also { page ->
                    networkMillis += SystemClock.elapsedRealtime() - beganRequest
                    observed.addAll(page.readings.map { it.timestamp })
                }
            } else {
            val index = ((query.window.until - window.since) / 60).coerceAtMost((count - 1).toLong())
            val rows = (index downTo maxOf(0, index - 1499)).map { step ->
                HistoryReading(window.since + step * 60, (step % 201).toString(16).padStart(2, '0'), null)
            }.filter { it.timestamp in query.window }
            HistoryPage(sensor, rows)
            }
        })
        lateinit var chart: LineChart
        var current: HistoryChartData? = null
        val began = SystemClock.elapsedRealtime()
        compose.setContent {
            RuuviTheme(darkTheme = true) {
                val state by controller.state.collectAsState()
                val units = remember { UnitsConverter(compose.activity, PreferencesRepository(Preferences(compose.activity))) }
                chart = remember { LineChart(compose.activity).also { applyChartStyle(compose.activity, it) } }
                val timeline = state.timeline ?: window
                val model = remember(state) {
                    HistoryChartData(state, "t", state.groups("t").map { group -> group.map { Entry(((it.timestamp - timeline.since) * 1000).toFloat(), it.value.toFloat(), it) } },
                        { controller.interaction(it) }, { from, to -> controller.move(from / 1000, to / 1000) })
                }
                SideEffect { current = model }
                LaunchedEffect(chart) { synchronizeChartGestures(setOf(chart)) { current } }
                val shared = remember { mutableStateOf<Float?>(null) }
                Column(Modifier.fillMaxSize().background(RuuviStationTheme.colors.background).padding(top = 24.dp)) {
                    HistoryDurationMenu(com.ruuvi.station.util.Period.All, 1096, true, null) {}
                    Row(Modifier.fillMaxWidth().height(36.dp)) {
                        HistorySyncControl(false, "", state.progress, {}, {}, Modifier.weight(1f))
                    }
                    HistoryStatus(state.error > 0, false, null, controller::refresh, {})
                    ChartViewPrototype(chart, Modifier.fillMaxWidth().height(420.dp), model.groups.flatten().toMutableList(), units,
                        UnitType.TemperatureUnit.Celsius, false, false, true, 10.0 to 25.0, timeline.since * 1000, timeline.until * 1000, shared, model)
                }
            }
        }
        compose.runOnIdle {
            controller.start(sensor, "synthetic-three-year", window) { bounds, refresh ->
                stream.snapshots(sensor, bounds, listOf("t"), HistoryPageCache.hash("synthetic-emulator"), "v1", refresh,
                    { _, _ -> HistoryLocalPage(emptyList(), null) }, decode)
            }
        }
        try {
            compose.waitUntil(if (live) 600000 else 180000) { controller.state.value.overview?.complete == true || controller.state.value.error > 0 }
            if (expected != null) println("Live Test coverage: expected=${expected.size}; observed=${observed.size}; missing=${(expected - observed).size}; extra=${(observed - expected).size}")
            compose.runOnIdle {
                assertEquals(count.toLong(), controller.state.value.overview?.readings)
                assertTrue(chart.data.entryCount <= 400)
                assertTrue(chart.xAxis.mEntryCount <= 12)
                assertEquals(0, controller.state.value.error.toInt())
            }
            if (expected != null) assertTrue("Live history must contain exactly the seeded eligible timestamps", expected == observed)
            val completedIn = SystemClock.elapsedRealtime() - began
            val overviewCalls = requests.get()
            pinch(chart, .10f, .40f)
            compose.waitUntil(60000) { controller.state.value.zoomed && controller.state.value.detail?.complete == true }
            val firstDetail = controller.state.value.visible
            compose.runOnIdle { assertTrue(chart.data.entryCount <= 800) }
            drag(chart)
            compose.waitUntil(60000) { controller.state.value.visible != firstDetail && controller.state.value.detail?.complete == true }
            repeat(3) { pinch(chart, .40f, .07f) }
            compose.waitUntil(60000) { !controller.state.value.zoomed }
            val afterReturn = requests.get()
            pinch(chart, .40f, .07f)
            compose.waitForIdle()
            assertEquals(afterReturn, requests.get())
            assertTrue(controller.state.value.overview?.complete == true)
            println("${if (live) "Live Test synthetic" else "Synthetic"} 1096-day graph: $count raw readings; overview ${completedIn}ms; pages=$overviewCalls; network=${networkMillis}ms; display<=800; zoom/pan/overview passed")
            if (live) runBlocking(Dispatchers.IO) {
                val callsBeforeWarm = requests.get()
                val warmStart = SystemClock.elapsedRealtime()
                val cached = stream.snapshots(sensor, window, listOf("t"), HistoryPageCache.hash("synthetic-emulator"), "v1", null,
                    { _, _ -> HistoryLocalPage(emptyList(), null) }, decode).last()
                assertEquals(count.toLong(), cached.plot.readings)
                assertEquals(callsBeforeWarm, requests.get())
                val warmMillis = SystemClock.elapsedRealtime() - warmStart
                val output = File(cacheFolder, "synthetic-history.csv")
                val exportStart = SystemClock.elapsedRealtime()
                var exported = 0L
                output.bufferedWriter().use { writer ->
                    writeHistoryCsvRow(writer, listOf("Timestamp", "Temperature"))
                    stream.read(sensor, window, HistoryPageCache.hash("synthetic-emulator"), null) { page, _ ->
                        for (row in page.readings) { writeHistoryCsvRow(writer, listOf(row.timestamp, decode(row).getValue("t"))); exported++ }
                    }
                }
                assertEquals(count.toLong(), exported)
                assertEquals(count + 1, output.useLines { it.count() })
                assertEquals(callsBeforeWarm, requests.get())
                println("Live Test synthetic cache: warm=${warmMillis}ms; CSV=${SystemClock.elapsedRealtime()-exportStart}ms; rows=$exported; extra HTTP=0")
            }
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            File(compose.activity.getExternalFilesDir(null), "synthetic-history.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG,100,it) }
            screenshot.recycle()
        } finally { compose.runOnIdle { controller.stop(true); scope.cancel() }; cacheFolder.deleteRecursively() }
    }

    private fun pinch(chart: LineChart, start: Float, end: Float) {
        val position = IntArray(2)
        instrumentation.runOnMainSync { chart.getLocationOnScreen(position) }
        val centerX = position[0] + chart.width * .5f; val y = position[1] + chart.height * .5f
        val down = SystemClock.uptimeMillis()
        val properties = Array(2) { MotionEvent.PointerProperties().apply { id = it; toolType = MotionEvent.TOOL_TYPE_FINGER } }
        fun send(action: Int, distance: Float, pointers: Int) {
            val coords = Array(pointers) { i -> MotionEvent.PointerCoords().apply { x = centerX + (if (i == 0) -1 else 1) * chart.width * distance; this.y = y; pressure = 1f; size = 1f } }
            val event = MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,pointers,properties,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
            instrumentation.sendPointerSync(event); event.recycle()
        }
        send(MotionEvent.ACTION_DOWN,start,1)
        send(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),start,2)
        repeat(12) { i -> send(MotionEvent.ACTION_MOVE,start + (end-start)*(i+1)/12,2); SystemClock.sleep(16) }
        send(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),end,2)
        send(MotionEvent.ACTION_UP,end,1)
        compose.waitForIdle()
    }
    private fun drag(chart: LineChart) {
        val location = IntArray(2); instrumentation.runOnMainSync { chart.getLocationOnScreen(location) }
        val down = SystemClock.uptimeMillis()
        for (i in 0..14) {
            val action = if (i == 0) MotionEvent.ACTION_DOWN else if (i == 14) MotionEvent.ACTION_UP else MotionEvent.ACTION_MOVE
            val event = MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,location[0]+chart.width*(.6f-i*.02f),location[1]+chart.height*.5f,0)
            event.source = InputDevice.SOURCE_TOUCHSCREEN; instrumentation.sendPointerSync(event); event.recycle(); SystemClock.sleep(16)
        }
        compose.waitForIdle()
    }
}
