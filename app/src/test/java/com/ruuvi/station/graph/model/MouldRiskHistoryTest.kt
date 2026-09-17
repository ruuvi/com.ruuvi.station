package com.ruuvi.station.graph.model

import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.vico.model.SegmentType
import org.junit.Assert.*
import org.junit.Test
import java.util.Date
import kotlin.math.roundToInt

class MouldRiskHistoryTest {
    private fun reading(time: Long, t: Double? = 20.0, rh: Double? = 80.0) =
        TagSensorReading(createdAt = Date(time), temperature = t, humidity = rh)

    @Test fun `invalid and missing readings break lines even for short gaps`() {
        val history = mouldRiskHistory(listOf(
            reading(0), reading(1000, rh = 90.0), reading(2000, rh = null),
            reading(3000), reading(4000, t = 0.0), reading(5000, rh = Double.NaN), reading(6000)
        ))
        assertEquals(listOf(listOf(0L, 1000L), listOf(3000L), listOf(6000L)), history.segments.map { it.timestamps })
        assertEquals(listOf(SegmentType.Solid, SegmentType.Single, SegmentType.Single), history.segments.map { it.segmentType })
        assertEquals(listOf(50.0, 75.0), history.segments.first().values)
    }

    @Test fun `long gaps break lines and raw precision is retained`() {
        val history = mouldRiskHistory(listOf(reading(0, t = 10.0), reading(3_600_001, t = 10.0)))
        assertEquals(2, history.segments.size)
        assertEquals(44.925, history.segments.first().values.single(), 0.000001)
        assertEquals(0.0, history.minValue, 0.0)
        assertEquals(100.0, history.maxValue, 0.0)
    }

    @Test fun `empty or entirely unavailable history contains no false zero points`() {
        assertTrue(mouldRiskHistory(emptyList()).segments.isEmpty())
        assertTrue(mouldRiskHistory(listOf(reading(0, rh = null), reading(1000, t = 50.0))).segments.isEmpty())
    }

    @Test fun `chart entries retain double precision at display rounding boundaries`() {
        val history = mouldRiskHistory(listOf(
            reading(0, rh = 79.7999996), reading(1000, rh = null), reading(2000, rh = 80.0)
        ))
        val entries = history.toMouldRiskEntries(0)
        // This valid score is just below 49.5 but its drawing coordinate rounds to 49.5f.
        assertEquals(50, entries.first().y.roundToInt())
        assertEquals(49, entries.first().preciseValue.roundToInt())
        assertEquals(history.segments.first().values.first(), entries.first().preciseValue, 0.0)
        assertTrue(entries.all { it.startsSegment })
        assertEquals(listOf(0f, 2000f), entries.map { it.x })
    }
}
