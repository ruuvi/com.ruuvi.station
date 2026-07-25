package com.ruuvi.station.widgets.ui.glance

import org.junit.Assert.assertEquals
import org.junit.Test

class GlanceFontUtilsTest {
    @Test
    fun `alpha mask keeps wider widget titles within the bitmap allocation cap`() {
        val allocationSafeWidth = calculateAllocationSafeBitmapWidth(bitmapHeight = 64)

        assertEquals(1_536, allocationSafeWidth)
        assertEquals(
            300,
            calculateBitmapMaxWidth(requestedWidth = 300, bitmapHeight = 64)
        )
        assertEquals(
            600,
            calculateBitmapMaxWidth(requestedWidth = 600, bitmapHeight = 64)
        )
        assertEquals(
            allocationSafeWidth,
            calculateBitmapMaxWidth(requestedWidth = 2_000, bitmapHeight = 64)
        )
    }

    @Test
    fun `truncated text always ends with an ellipsis`() {
        assertEquals("Bedroom…", ensureTrailingEllipsis("Bedroom", isTruncated = true))
        assertEquals("Bedroom…", ensureTrailingEllipsis("Bedroom…", isTruncated = true))
        assertEquals("…", ensureTrailingEllipsis("", isTruncated = true))
    }

    @Test
    fun `text that fits is unchanged`() {
        assertEquals("Bedroom", ensureTrailingEllipsis("Bedroom", isTruncated = false))
    }
}
