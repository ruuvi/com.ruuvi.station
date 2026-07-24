package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetLayoutConfigTest {
    @Test
    fun `short widget uses compact geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 40.dp)

        assertEquals(8.dp, config.horizontalPadding)
        assertEquals(4.dp, config.verticalPadding)
    }

    @Test
    fun `tall widget uses roomy geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 90.dp)

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(8.dp, config.verticalPadding)
    }

    @Test
    fun `display zoom scales geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(
            height = 40.dp,
            zoomFactor = 2f
        )

        assertEquals(4.dp, config.horizontalPadding)
        assertEquals(2.dp, config.verticalPadding)
    }

    @Test
    fun `one cell widget gives 15 colon 15 more width than touch target subtraction`() {
        val oneCellWidth = 82.dp
        val contentPadding = 8.dp
        val touchTargetSize = 44.dp
        val refreshVisualEndInset = 30.dp
        val inlineGap = 1.dp

        val previousWidth =
            oneCellWidth - (contentPadding * 2) - touchTargetSize
        val timestampWidth = calculateTimestampMaxWidth(
            widgetWidth = oneCellWidth,
            contentStartPadding = contentPadding,
            refreshVisualEndInset = refreshVisualEndInset,
            inlineGap = inlineGap
        )

        assertEquals(43.dp, timestampWidth)
        assertTrue(
            "15:15 should receive more width than subtracting the whole touch target",
            timestampWidth > previousWidth
        )
    }
}
