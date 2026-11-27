package com.ruuvi.station.history

import android.app.Application
import com.google.gson.JsonParser
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.export.writeHistoryCsvRow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.last
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Date

/** Explicit, reserved Test-only run. The private session configuration is never a test resource. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class HistoryLiveTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun `deployed synthetic history preserves all readings through warm cache and CSV`() = runBlocking {
        val path = System.getenv("RUUVI_ANDROID_HISTORY_LIVE_CONFIG")
        assumeTrue("Explicit private Test fixture required", !path.isNullOrEmpty())
        val fixture = JsonParser.parseString(File(requireNotNull(path)).readText()).asJsonObject
        require(fixture.get("synthetic").asBoolean && fixture.get("base").asString == "https://testnet.ruuvi.com")
        val sensor = fixture.get("sensor").asString
        val now = System.currentTimeMillis()
        val window = HistoryWindow(now / 1000 - 1096 * 86400 + 3600, now / 1000)
        val split = now / 1000 - 86400
        val expected = mutableSetOf<Long>()
        for (time in fixture.get("seedStart").asLong..fixture.get("seedEnd").asLong step 900)
            if (time in window && time <= split) expected += time
        for (time in fixture.get("denseEnd").asLong - 86400..fixture.get("denseEnd").asLong step 60)
            if (time in window && time > split) expected += time
        val transport = HistoryTransport()
        val observed = mutableSetOf<Long>()
        var calls = 0; var networkMillis = 0L
        val stream = HistoryStream(HistoryPageCache(folder.newFolder("cache")), { query ->
            calls++
            val start = System.nanoTime()
            try {
                transport.load(query, "https://testnet.ruuvi.com/", fixture.get("token").asString).also { page ->
                    observed.addAll(page.readings.map { it.timestamp })
                    if (calls % 10 == 0) println("Live synthetic pages=$calls")
                }
            } catch (error: Exception) {
                println("Live synthetic request failure: ${error.javaClass.simpleName}; status=${(error as? HistoryFailure.Http)?.status}")
                throw error
            } finally { networkMillis += (System.nanoTime() - start) / 1_000_000 }
        }, { now })
        val scope = HistoryPageCache.hash("live-synthetic-verification")
        val decode: HistoryDecode = { row ->
            val value = TagSensorReading(BluetoothLibrary.decode(sensor, row.data, row.rssi ?: 0), Date(row.timestamp * 1000))
            mapOf("t" to requireNotNull(value.temperature), "h" to requireNotNull(value.humidity), "p" to requireNotNull(value.pressure))
        }
        val start = System.nanoTime()
        val cold = stream.snapshots(sensor, window, listOf("t", "h", "p"), scope, "live-v1", null,
            { _, _ -> HistoryLocalPage(emptyList(), null) }, decode).last()
        val coldMillis = (System.nanoTime() - start) / 1_000_000
        println("Live synthetic coverage: expected=${expected.size}; observed=${observed.size}; missing=${(expected-observed).size}; extra=${(observed-expected).size}")
        assertTrue("Every eligible synthetic timestamp must be preserved", expected == observed)
        assertEquals(expected.size.toLong(), cold.plot.readings)
        assertTrue(cold.plot.samples.values.all { it.size <= 400 })
        val coldCalls = calls
        val warmStart = System.nanoTime()
        val warm = stream.snapshots(sensor, window, listOf("t", "h", "p"), scope, "live-v1", null,
            { _, _ -> HistoryLocalPage(emptyList(), null) }, decode).last()
        val warmMillis = (System.nanoTime() - warmStart) / 1_000_000
        assertEquals(cold.plot, warm.plot)
        assertEquals(coldCalls, calls)
        val exportStart = System.nanoTime()
        val csv = folder.newFile("synthetic.csv")
        var exported = 0L
        csv.bufferedWriter().use { writer ->
            writeHistoryCsvRow(writer, listOf("Timestamp", "Temperature"))
            stream.read(sensor, window, scope, null) { page, _ ->
                for (row in page.readings) { writeHistoryCsvRow(writer, listOf(row.timestamp, decode(row).getValue("t"))); exported++ }
            }
        }
        assertEquals(expected.size.toLong(), exported)
        assertEquals(expected.size + 1, csv.useLines { it.count() })
        assertEquals(coldCalls, calls)
        println("Live synthetic: readings=$exported; pages=$coldCalls; cold=${coldMillis}ms; network=${networkMillis}ms; warm=${warmMillis}ms; CSV=${(System.nanoTime()-exportStart)/1_000_000}ms; extra HTTP=0")
    }
}
