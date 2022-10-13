package com.ruuvi.station

import com.ruuvi.station.util.extensions.round
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundTest {
    @Test
    fun testDoubleRound() {
        val testValues = listOf(
            Triple(29.015, 29.02, 2),
            Triple(35.925, 35.93, 2),
            Triple(28.415, 28.42, 2),
            Triple(1.444, 1.44, 2),
            Triple(1.002, 1.0, 2),
            Triple(1.67, 1.7, 1),
            Triple(1.5555, 1.56, 2),
            Triple(-1.505, -1.51, 2),
            Triple(0.0, 0.0, 2),
            Triple(1.5, 2.0, 0),
            Triple(1.49, 1.0, 0)
        )

        for (value in testValues) {
            System.out.println("${value.first} -> ${value.first.round(value.third)} Expected ${value.second}")
            assertEquals(value.second, value.first.round(value.third), 0.000001)
        }
    }
}