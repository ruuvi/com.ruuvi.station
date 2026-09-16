package com.ruuvi.station.history

import org.junit.Assert.*
import org.junit.Test

class HistorySamplerTest {
    @Test fun `empty nonfinite and out of range measurements are ignored`() {
        val sampler = HistorySampler(HistoryRange(100, 200))
        sampler.add(99, 1.0); sampler.add(200, 2.0)
        sampler.add(120, null); sampler.add(130, Double.NaN); sampler.add(140, Double.POSITIVE_INFINITY)
        assertEquals(SampledHistory(emptyList(), null), sampler.finish())
    }

    @Test fun `up to the point cap measurements are preserved exactly including boundaries`() {
        val count = HistorySampler.MAX_POINTS
        val sampler = HistorySampler(HistoryRange(0, count.toLong()))
        repeat(count) { sampler.add(it.toLong(), -it.toDouble()) }
        val result = sampler.finish()
        assertEquals((0L until count.toLong()).toList(), result.points.map { it.timestamp })
        assertEquals(-(count - 1).toDouble(), result.statistics!!.minimum, 0.0)
        assertEquals(-(count - 1) / 2.0, result.statistics.average, 0.0)
    }

    @Test fun `dense one minute and 100 day ranges both obey the unconditional cap`() {
        for (duration in listOf(60_000L, 100 * 86_400_000L)) {
            val sampler = HistorySampler(HistoryRange(0, duration))
            repeat(144000) { i -> sampler.add(i * duration / 144000, kotlin.math.sin(i.toDouble())) }
            val result = sampler.finish()
            assertTrue(result.points.size <= HistorySampler.MAX_POINTS)
            assertEquals(0L, result.points.first().timestamp)
            assertEquals(143999L * duration / 144000, result.points.last().timestamp)
            assertEquals(144000L, result.statistics!!.count)
            assertTrue(result.points.zipWithNext().all { (a, b) -> a.timestamp <= b.timestamp })
        }
    }

    @Test fun `bucket minima maxima and short spikes survive sampling`() {
        val sampler = HistorySampler(HistoryRange(0, 10000))
        repeat(10000) { i -> sampler.add(i.toLong(), when (i) { 2345 -> 90.0; 2346 -> -70.0; else -> 2.0 }) }
        val result = sampler.finish()
        assertTrue(result.points.any { it.timestamp == 2345L && it.value == 90.0 })
        assertTrue(result.points.any { it.timestamp == 2346L && it.value == -70.0 })
        assertEquals(90.0, result.statistics!!.maximum, 0.0)
        assertEquals(-70.0, result.statistics.minimum, 0.0)
        assertEquals((2.0 * 9999 + 88 - 72) / 9999, result.statistics.average, 1e-10)
    }

    @Test fun `sampling intervals do not create gaps but raw outages do`() {
        val sampler = HistorySampler(HistoryRange(0, 100 * 86_400_000L))
        repeat(144000) { i -> if (i !in 60000..61000) sampler.add(i * 60_000L, 20.0) }
        val points = sampler.finish().points
        assertEquals(setOf(0, 1), points.map { it.segment }.toSet())
        assertTrue(points.zipWithNext().any { (a, b) -> a.segment == b.segment && b.timestamp - a.timestamp > HistorySampler.GAP_MILLIS })
        assertTrue(points.filter { it.segment == 0 }.all { it.timestamp < 60000L * 60_000 })
        assertTrue(points.filter { it.segment == 1 }.all { it.timestamp > 61000L * 60_000 })
    }

    @Test fun `zoomed local window reveals measurements omitted from overview`() {
        val all = HistorySampler(HistoryRange(0, 100000))
        val zoom = HistorySampler(HistoryRange(20000, 21000))
        repeat(100000) { i -> all.add(i.toLong(), i.toDouble()); zoom.add(i.toLong(), i.toDouble()) }
        val overview = all.finish().points.map { it.timestamp }.toSet()
        val detail = zoom.finish().points
        assertTrue(detail.size <= HistorySampler.MAX_POINTS)
        assertTrue(detail.any { it.timestamp !in overview })
        assertTrue(detail.all { it.timestamp in 20000L until 21000L })
    }
}
