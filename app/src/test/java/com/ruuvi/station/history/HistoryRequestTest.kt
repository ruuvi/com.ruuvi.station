package com.ruuvi.station.history

import com.ruuvi.station.network.data.request.SensorDataMode
import org.junit.Assert.*
import org.junit.Test

class HistoryRequestTest {
    private val now = 1_800_000_000_123L
    private val day = 86_400_000L

    @Test fun `90 days and longer use large pages even for a short missing interval`() {
        val missing = HistoryRange(now - 10_000, now)
        for (days in listOf(89, 90, 180, 365, 730, 1095)) {
            val selected = HistoryRange(now - days * day, now)
            val plan = HistoryRequest.plan(listOf(missing), selected, now)
            assertEquals(if (days >= 90) 144_000 else 5000, plan.single().limit)
            assertEquals(missing, plan.single().range)
        }
    }

    @Test fun `archive and recent requests meet at a whole second without gaps`() {
        val range = HistoryRange.retained(now)
        val plan = HistoryRequest.plan(listOf(range), range, now)
        assertEquals(2, plan.size)
        assertEquals(SensorDataMode.SPARSE, plan[0].mode)
        assertEquals(SensorDataMode.MIXED, plan[1].mode)
        assertEquals(range.startMillis, plan[0].range.startMillis)
        assertEquals(range.endExclusiveMillis, plan[1].range.endExclusiveMillis)
        assertEquals(plan[0].range.endExclusiveMillis, plan[1].range.startMillis)
        assertEquals(0L, plan[1].range.startMillis % 1000)
    }

    @Test fun `historical only and recent only selections do not produce empty requests`() {
        val old = HistoryRange(now - 400 * day, now - 300 * day)
        val recent = HistoryRange(now - day, now)
        assertEquals(SensorDataMode.SPARSE, HistoryRequest.plan(listOf(old), old, now).single().mode)
        assertEquals(SensorDataMode.MIXED, HistoryRequest.plan(listOf(recent), recent, now).single().mode)
        assertTrue(HistoryRequest.plan(listOf(HistoryRange(now, now)), recent, now).isEmpty())
    }

    @Test fun `reopening reuses archive and live polling overlaps just one minute`() {
        val range = HistoryRange.retained(now)
        assertEquals(listOf(HistoryRange(now - 2 * day, now)), range.historyToFetch(listOf(range), true, now))
        assertEquals(listOf(HistoryRange(now - 60_000, now)), range.historyToFetch(listOf(range), false, now))
        val older = HistoryRange(now - 700 * day, now - 600 * day)
        assertTrue(older.historyToFetch(listOf(older), true, now).isEmpty())
    }

    @Test fun `incomplete archive stays missing alongside the recent revalidation window`() {
        val range = HistoryRange.retained(now)
        val covered = HistoryRange(range.startMillis, now - 20 * day)
        assertEquals(listOf(HistoryRange(covered.endExclusiveMillis, now)), range.historyToFetch(listOf(covered), true, now))
    }
}
