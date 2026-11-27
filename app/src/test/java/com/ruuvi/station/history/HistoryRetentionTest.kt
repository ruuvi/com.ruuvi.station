package com.ruuvi.station.history

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import io.mockk.*
import com.ruuvi.station.database.domain.*
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.tagdetails.domain.TagDetailsInteractor

class HistoryRetentionTest {
    @get:Rule val temporary = TemporaryFolder()
    @Test fun `local readers cap a duration saved on a three year cloud sensor`() {
        val history = mockk<SensorHistoryRepository>()
        val settings = mockk<SensorSettingsRepository>(relaxed = true)
        val preferences = mockk<PreferencesRepository>(relaxed = true)
        every { preferences.isShowAllGraphPoint() } returns true
        every { history.getHistory("local", 240) } returns emptyList()
        val interactor = TagDetailsInteractor(mockk(), history, settings, mockk(), preferences)
        assertTrue(interactor.getTagReadings("local", 1096 * 24).isEmpty())
        verify(exactly = 1) { history.getHistory("local", 240) }
    }
    @Test fun `cache retention trims raw rows values and coverage for every session while preserving other sensors`() = runBlocking {
        val root = temporary.newFolder()
        val cache = HistoryPageCache(root)
        val sensor = "AA:BB:CC:DD:EE:FF"
        val other = "11:22:33:DD:EE:FF"
        val scopes = listOf(HistoryPageCache.hash("a"), HistoryPageCache.hash("b"))
        val rows = (1000L downTo 600 step 100).map { HistoryReading(it, "00", null) }
        for (scope in scopes) {
            cache.save(HistoryPage(sensor, rows), HistoryQuery(sensor, HistoryWindow(0, 1000), HistoryMode.SPARSE),
                HistoryWindow(0, 1000), scope, 1000, "v1", rows.map { mapOf("t" to it.timestamp.toDouble()) })
        }
        cache.save(HistoryPage(other, rows), HistoryQuery(other, HistoryWindow(0, 1000), HistoryMode.SPARSE), HistoryWindow(0, 1000), scopes[0], 1000)
        cache.prune(sensor, 900)
        val reopened = HistoryPageCache(root)
        for (scope in scopes) {
            val entry = reopened.candidates(scope, sensor, 1000).single()
            assertEquals(900L, entry.lower); assertEquals(1000L, entry.upper)
            val saved = requireNotNull(reopened.read(entry, sensor))
            assertEquals(listOf(1000L, 900L), saved.page.readings.map { it.timestamp })
            assertEquals(listOf(1000.0, 900.0), saved.values!!.map { it.getValue("t") })
        }
        assertEquals(0L, reopened.candidates(scopes[0], other, 1000).single().lower)
        reopened.prune(sensor, 1001)
        assertTrue(reopened.candidates(scopes[0], sensor, 1000).isEmpty())
        assertEquals(1, reopened.candidates(scopes[0], other, 1000).size)
    }
}
