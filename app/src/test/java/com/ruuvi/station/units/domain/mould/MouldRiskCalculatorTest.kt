package com.ruuvi.station.units.domain.mould

import org.junit.Assert.*
import org.junit.Test

class MouldRiskCalculatorTest {
    @Test fun `room temperature examples and clamping`() {
        listOf(0.0 to 0.0, 50.0 to 0.0, 60.0 to 0.0, 70.0 to 25.0,
            80.0 to 50.0, 90.0 to 75.0, 100.0 to 100.0).forEach { (rh, expected) ->
            assertEquals(expected, MouldRiskCalculator.calculate(20.0, rh).score!!, 0.000001)
        }
    }

    @Test fun `cold reference curve and twenty degree transition`() {
        assertEquals(29.959375, MouldRiskCalculator.calculate(5.0, 80.0).score!!, 0.000001)
        assertEquals(44.925, MouldRiskCalculator.calculate(10.0, 80.0).score!!, 0.000001)
        assertEquals(50.0, MouldRiskCalculator.calculate(17.0, 80.0).score!!, 0.000001)
        assertEquals(50.0, MouldRiskCalculator.calculate(20.0, 80.0).score!!, 0.000001)
        assertEquals(50.0, MouldRiskCalculator.calculate(49.999, 80.0).score!!, 0.000001)
        assertNotNull(MouldRiskCalculator.calculate(0.001, 100.0).score)
        assertNotNull(MouldRiskCalculator.calculate(19.999, 80.0).score)
    }

    @Test fun `invalid input never becomes a zero score`() {
        listOf(null to 80.0, 20.0 to null, null to null).forEach { (t, rh) ->
            assertEquals(MouldRiskResult.Unavailable(MouldRiskUnavailableReason.MISSING_INPUT),
                MouldRiskCalculator.calculate(t, rh))
        }
        listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY).forEach { invalid ->
            assertNull(MouldRiskCalculator.calculate(invalid, 80.0).score)
            assertNull(MouldRiskCalculator.calculate(20.0, invalid).score)
        }
        listOf(-0.01, 100.01).forEach { rh ->
            assertEquals(MouldRiskResult.Unavailable(MouldRiskUnavailableReason.INVALID_INPUT),
                MouldRiskCalculator.calculate(20.0, rh))
        }
        listOf(-20.0, 0.0, 50.0, 60.0).forEach { t ->
            assertEquals(MouldRiskResult.Unavailable(MouldRiskUnavailableReason.TEMPERATURE_OUT_OF_RANGE),
                MouldRiskCalculator.calculate(t, 80.0))
        }
    }

    @Test fun `display and category use the same half up rounding at every boundary`() {
        val boundaries = listOf(
            9.499 to MouldRiskLevel.VERY_LOW, 9.5 to MouldRiskLevel.LOW,
            24.499 to MouldRiskLevel.LOW, 24.5 to MouldRiskLevel.ELEVATED,
            49.499 to MouldRiskLevel.ELEVATED, 49.5 to MouldRiskLevel.HIGH,
            74.499 to MouldRiskLevel.HIGH, 74.5 to MouldRiskLevel.VERY_HIGH,
            0.0 to MouldRiskLevel.VERY_LOW, 100.0 to MouldRiskLevel.VERY_HIGH
        )
        boundaries.forEach { (score, level) ->
            val result = MouldRiskResult.Available(score)
            assertEquals(level, result.level)
            assertEquals(level, MouldRiskLevel.fromScore(result.displayedScore.toDouble()))
        }
        assertEquals(50, MouldRiskResult.Available(49.5).displayedScore)
        assertEquals(100, MouldRiskResult.Available(99.5).displayedScore)
    }

    @Test fun `score is bounded and monotonic in humidity throughout supported temperatures`() {
        for (temperature in 1..49) {
            val scores = (0..100).map { MouldRiskCalculator.calculate(temperature.toDouble(), it.toDouble()).score!! }
            assertTrue(scores.all { it in 0.0..100.0 })
            assertTrue(scores.zipWithNext().all { (a, b) -> a <= b })
        }
    }
}
