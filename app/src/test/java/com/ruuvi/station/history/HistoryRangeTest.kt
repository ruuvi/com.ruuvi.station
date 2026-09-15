package com.ruuvi.station.history

import com.ruuvi.station.app.preferences.GlobalSettings
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class HistoryRangeTest {
    private val now = Instant.parse("2026-09-15T12:00:00Z").toEpochMilli()

    @Test fun `All and 100 days use the same overflow safe rolling range`() {
        val all = HistorySelection.Rolling().resolve(now)
        assertEquals(8_640_000_000L, now - all.startMillis)
        assertEquals(GlobalSettings.historyLengthMillis, now - all.startMillis)
        assertEquals(all, HistorySelection.Rolling(2400).resolve(now))
        assertEquals(now, all.endExclusiveMillis)
    }

    @Test fun `shortcuts stay within their selected duration`() {
        for (hours in listOf(1, 7 * 24, 30 * 24, 60 * 24)) {
            assertEquals(HistoryRange(now - hours * 3_600_000L, now), HistorySelection.Rolling(hours).resolve(now))
        }
    }

    @Test fun `custom selection includes entire end date in local timezone`() {
        val range = HistorySelection.Custom(date("2026-08-01"), date("2026-08-07"))
            .resolve(now, TimeZone.getTimeZone("Europe/Helsinki"))
        assertEquals(Instant.parse("2026-07-31T21:00:00Z").toEpochMilli(), range.startMillis)
        assertEquals(Instant.parse("2026-08-07T21:00:00Z").toEpochMilli(), range.endExclusiveMillis)
    }

    @Test fun `same day spring and autumn DST ranges have 23 and 25 hours`() {
        val zone = TimeZone.getTimeZone("Europe/Helsinki")
        for ((day, expected) in listOf("2026-03-29" to 23L, "2026-10-25" to 25L)) {
            val range = HistorySelection.Custom(date(day), date(day)).resolve(date(day) + 3 * 86_400_000L, zone)
            assertEquals(expected * 3_600_000, range.endExclusiveMillis - range.startMillis)
        }
    }

    @Test fun `today is capped at now and oldest partial day at retention cutoff`() {
        val range = HistorySelection.Custom(date("2026-06-07"), date("2026-09-15"))
            .resolve(now, TimeZone.getTimeZone("UTC"))
        assertEquals(HistoryRange.retained(now), range)
    }

    @Test fun `expired selection and disjoint intersection are empty`() {
        assertTrue(HistorySelection.Custom(date("2025-01-01"), date("2025-01-01")).resolve(now).isEmpty)
        assertTrue(HistoryRange(1, 3).intersect(HistoryRange(10, 20)).isEmpty)
    }

    @Test fun `calendar conversion preserves local dates across UTC boundaries`() {
        val zone = TimeZone.getTimeZone("Asia/Kathmandu")
        val instant = Instant.parse("2026-09-14T23:30:00Z").toEpochMilli()
        assertEquals(date("2026-09-15"), calendarDateUtc(instant, zone))
    }

    @Test fun `coverage subtraction handles overlap nesting adjacency and disjoint windows`() {
        val request = HistoryRange(0, 100)
        val covered = listOf(HistoryRange(50, 70), HistoryRange(10, 30), HistoryRange(20, 25), HistoryRange(30, 40))
        assertEquals(listOf(HistoryRange(0, 10), HistoryRange(40, 50), HistoryRange(70, 100)), uncoveredHistory(request, covered))
        assertEquals(listOf(request), uncoveredHistory(request, listOf(HistoryRange(110, 120))))
        assertTrue(uncoveredHistory(request, listOf(HistoryRange(-10, 110))).isEmpty())
        assertTrue(uncoveredHistory(HistoryRange(0, 0), emptyList()).isEmpty())
    }

    private fun date(day: String) = Instant.parse("${day}T00:00:00Z").toEpochMilli()
}
