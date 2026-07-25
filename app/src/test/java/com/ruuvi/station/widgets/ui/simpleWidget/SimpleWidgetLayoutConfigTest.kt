package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetLayoutConfigTest {
    @Test
    fun `short widget uses compact geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 40.dp)

        assertEquals(16.dp, config.horizontalPadding)
        assertEquals(16.dp, config.verticalPadding)
    }

    @Test
    fun `tall widget uses roomy geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 90.dp)

        assertEquals(16.dp, config.horizontalPadding)
        assertEquals(16.dp, config.verticalPadding)
    }

    @Test
    fun `display zoom scales geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(
            height = 40.dp,
            zoomFactor = 2f
        )

        assertEquals(8.dp, config.horizontalPadding)
        assertEquals(8.dp, config.verticalPadding)
    }

    @Test
    fun `one cell timestamp stops before the refresh backing`() {
        val oneCellWidth = 82.dp
        val contentPadding = 16.dp
        val touchTargetSize = 44.dp
        val refreshBackingEndInset = 34.dp
        val inlineGap = 1.dp

        val previousWidth =
            oneCellWidth - (contentPadding * 2) - touchTargetSize
        val timestampWidth = calculateTimestampMaxWidth(
            widgetWidth = oneCellWidth,
            contentStartPadding = contentPadding,
            refreshEndInset = refreshBackingEndInset,
            inlineGap = inlineGap
        )

        assertEquals(31.dp, timestampWidth)
        assertTrue(
            "15:15 should receive more width than subtracting the whole touch target",
            timestampWidth > previousWidth
        )
    }

    @Test
    fun `all rows remain visible when their natural heights fit`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 80.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 15.dp,
                secondary = 7.dp
            ),
            hasUnit = true,
            isAirQuality = false
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `short widget hides optional rows instead of shrinking text`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 60.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 60.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 15.dp,
                secondary = 7.dp
            ),
            hasUnit = true,
            isAirQuality = false
        )

        assertTrue(visibility.showDisplayName)
        assertEquals(false, visibility.showMeasurementDescription)
        assertEquals(false, visibility.showTimestamp)
    }

    @Test
    fun `very large text leaves only the measurement value`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 45.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 45.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 20.dp,
                value = 28.dp,
                secondary = 12.dp
            ),
            hasUnit = true,
            isAirQuality = false
        )

        assertEquals(false, visibility.showDisplayName)
        assertEquals(false, visibility.showMeasurementDescription)
        assertEquals(false, visibility.showTimestamp)
    }

    @Test
    fun `air quality progress is hidden before text rows are compressed`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 80.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 18.dp,
                secondary = 7.dp
            ),
            hasUnit = false,
            isAirQuality = true
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showMeasurementDescription)
        assertEquals(false, visibility.showAqiProgress)
        assertTrue(visibility.showTimestamp)
    }
}
