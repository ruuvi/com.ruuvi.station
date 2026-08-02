package com.ruuvi.station.widgets.ui.glance

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Test

class GlanceFontUtilsTest {
    @Test
    fun `runtime text uses a one-byte alpha mask`() {
        val format = fontBitmapFormat(embeddedColor = null)

        assertEquals(Bitmap.Config.ALPHA_8, format.config)
        assertEquals(1, format.bytesPerPixel)
    }

    @Test
    fun `generated preview text embeds color in a four-byte bitmap`() {
        val format = fontBitmapFormat(embeddedColor = 0x12345678)

        assertEquals(Bitmap.Config.ARGB_8888, format.config)
        assertEquals(4, format.bytesPerPixel)
    }

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
    fun `colored preview bitmap accounts for four bytes per pixel`() {
        val allocationSafeWidth = calculateAllocationSafeBitmapWidth(
            bitmapHeight = 64,
            bytesPerPixel = 4
        )

        assertEquals(384, allocationSafeWidth)
        assertEquals(
            300,
            calculateBitmapMaxWidth(
                requestedWidth = 300,
                bitmapHeight = 64,
                bytesPerPixel = 4
            )
        )
        assertEquals(
            allocationSafeWidth,
            calculateBitmapMaxWidth(
                requestedWidth = 600,
                bitmapHeight = 64,
                bytesPerPixel = 4
            )
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
