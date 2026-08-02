package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AqiProgressGeometryTest {
    @Test
    fun `narrow widget keeps track at least as wide as indicator`() {
        val geometry = calculateAqiProgressGeometry(
            requestedWidth = 2.dp,
            glowSize = 12.dp,
            progress = 0.5f
        )

        assertEquals(12.dp, geometry.totalWidth)
        assertEquals(6.dp, geometry.activeWidth)
        assertEquals(6.dp, geometry.inactiveWidth)
        assertEquals(0.dp, geometry.dotOffset)
    }

    @Test
    fun `invalid progress is treated as zero`() {
        val geometry = calculateAqiProgressGeometry(
            requestedWidth = 80.dp,
            glowSize = 12.dp,
            progress = Float.NaN
        )

        assertEquals(0.dp, geometry.activeWidth)
        assertEquals(80.dp, geometry.inactiveWidth)
        assertEquals(0.dp, geometry.dotOffset)
    }

    @Test
    fun `wide track and overflowing progress are clamped`() {
        val geometry = calculateAqiProgressGeometry(
            requestedWidth = 180.dp,
            glowSize = 12.dp,
            progress = 2f
        )

        assertEquals(100.dp, geometry.totalWidth)
        assertEquals(100.dp, geometry.activeWidth)
        assertEquals(0.dp, geometry.inactiveWidth)
        assertEquals(88.dp, geometry.dotOffset)
    }
}
