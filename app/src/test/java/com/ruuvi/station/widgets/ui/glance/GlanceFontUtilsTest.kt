package com.ruuvi.station.widgets.ui.glance

import org.junit.Assert.assertEquals
import org.junit.Test

class GlanceFontUtilsTest {
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
