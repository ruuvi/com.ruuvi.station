package com.ruuvi.station.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SmoothAlphaCurveTest {

    private val sampleAlphas = listOf(
        intArrayOf(2, 1, 13, 46, 87, 123, 151, 175),
        intArrayOf(149, 108, 72, 41, 18, 2, 1, 2),
        intArrayOf(6, 9, 40, 79, 113, 142, 160, 170),
        intArrayOf(170, 118, 66, 25, 23, 42, 74, 102),
    )

    /** Value of the smoothed curve at [u], in units of the design gradient's length. */
    private fun SmoothedCurve.at(u: Float): Float {
        val i = ((u - start) / (end - start) * (values.size - 1)).toInt().coerceIn(0, values.size - 1)
        return values[i]
    }

    @Test
    fun `curve stays close to the design at every stop`() {
        sampleAlphas.forEach { alphas ->
            listOf(true, false).forEach { mirror ->
                val curve = smoothedAlphaCurve(alphas, mirror, stopCount = 2001)
                alphas.forEachIndexed { k, alpha ->
                    // A radial centre is a point in the design; smoothing rounds it off (below).
                    if (mirror && k == 0) return@forEachIndexed
                    val u = k / (alphas.size - 1f)
                    // Smoothing a corner shifts it by about 0.4 x slope jump x smoothing width.
                    val n = alphas.size - 1
                    val slopeIn = if (k == 0) 0 else alphas[k] - alphas[k - 1]
                    val slopeOut = if (k == n) 0 else alphas[k + 1] - alphas[k]
                    val tolerance = 0.45f * abs(slopeOut - slopeIn) * n * SMOOTHING + 1f
                    assertEquals("stop $k of ${alphas.toList()} mirror=$mirror", alpha.toFloat(), curve.at(u), tolerance)
                }
            }
        }
    }

    @Test
    fun `curve settles to the design end value before the shader clamps`() {
        sampleAlphas.forEach { alphas ->
            listOf(true, false).forEach { mirror ->
                val curve = smoothedAlphaCurve(alphas, mirror)
                assertTrue(curve.end > 1f)
                assertEquals("end of ${alphas.toList()}", alphas.last().toFloat(), curve.values.last(), 0.05f)
                // Slope at the very end is ~0, so meeting the clamped colour leaves no crease.
                val n = curve.values.size
                assertEquals(0f, curve.values[n - 1] - curve.values[n - 2], 0.01f)
            }
        }
    }

    @Test
    fun `linear curve also settles to the design start value`() {
        sampleAlphas.forEach { alphas ->
            val curve = smoothedAlphaCurve(alphas, mirrorAtStart = false)
            assertTrue(curve.start < 0f)
            assertEquals(alphas.first().toFloat(), curve.values.first(), 0.05f)
            assertEquals(0f, curve.values[1] - curve.values[0], 0.01f)
        }
    }

    @Test
    fun `radial curve starts at its centre and rounds the point off`() {
        sampleAlphas.forEach { alphas ->
            val curve = smoothedAlphaCurve(alphas, mirrorAtStart = true)
            assertEquals(0f, curve.start)
            // The centre moves towards its neighbouring stop instead of keeping a sharp point...
            val low = minOf(alphas[0], alphas[1]).toFloat()
            val high = maxOf(alphas[0], alphas[1]).toFloat()
            assertTrue("centre of ${alphas.toList()}: ${curve.values[0]}", curve.values[0] in low..high)
            // ...and the slope starts from zero there.
            val du = (curve.end - curve.start) / (curve.values.size - 1)
            val firstSlope = abs(curve.values[1] - curve.values[0]) / du
            val designSlope = abs(alphas[1] - alphas[0]) * (alphas.size - 1f)
            assertTrue("centre slope of ${alphas.toList()}: $firstSlope", firstSlope <= designSlope * 0.1f)
        }
    }

    @Test
    fun `curve has no sharp bends anywhere`() {
        sampleAlphas.forEach { alphas ->
            listOf(true, false).forEach { mirror ->
                val curve = smoothedAlphaCurve(alphas, mirror)
                val du = (curve.end - curve.start) / (curve.values.size - 1)
                val slopes = FloatArray(curve.values.size - 1) { (curve.values[it + 1] - curve.values[it]) / du }
                val maxChange = (0 until slopes.size - 1).maxOf { abs(slopes[it + 1] - slopes[it]) }
                // Joined by straight lines and clamped, the design's slope jumps at its corners
                // (between segments, at the end, and at the start or radial centre).
                val n = alphas.size - 1
                val corners = (1 until n).map { abs((alphas[it + 1] - alphas[it]) - (alphas[it] - alphas[it - 1])) * n } +
                    abs(alphas[n] - alphas[n - 1]) * n +
                    abs(alphas[1] - alphas[0]) * n * (if (mirror) 2 else 1)
                val biggestCorner = corners.max().toFloat()
                // Smoothing spreads every corner over many GPU stops.
                assertTrue(
                    "${alphas.toList()} mirror=$mirror: $maxChange per stop vs corner $biggestCorner",
                    maxChange <= biggestCorner * 0.06f,
                )
            }
        }
    }

    @Test
    fun `flat input stays flat`() {
        val curve = smoothedAlphaCurve(intArrayOf(40, 40, 40, 40), mirrorAtStart = false)
        curve.values.forEach { assertEquals(40f, it, 1e-3f) }
    }
}
