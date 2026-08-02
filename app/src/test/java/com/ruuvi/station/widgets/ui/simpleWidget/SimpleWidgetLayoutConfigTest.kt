package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetLayoutConfigTest {
    @Test
    fun `short widget uses compact geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 40.dp)

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(8.dp, config.verticalPadding)
        assertEquals(9.5.sp, config.displayNameFontSize)
        assertEquals(12.sp, config.valueFontSize)
    }

    @Test
    fun `tall widget uses roomy geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 90.dp)

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(12.dp, config.verticalPadding)
        assertEquals(16.sp, config.displayNameFontSize)
    }

    @Test
    fun `medium widget uses compact sensor name`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp)

        assertEquals(11.sp, config.displayNameFontSize)
    }

    @Test
    fun `display zoom does not change selected layout geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(
            height = 40.dp,
            zoomFactor = 2f
        )

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(8.dp, config.verticalPadding)
        assertEquals(9.5.sp, config.displayNameFontSize)
    }

    @Test
    fun `wide short widget has dedicated balanced style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 110.dp,
            isTablet = true
        )

        assertEquals(SimpleWidgetLayoutStyle.BALANCED_SHORT, config.style)
        assertEquals(10.sp, config.displayNameFontSize)
        assertEquals(14.sp, config.valueFontSize)
        assertEquals(8.sp, config.secondaryFontSize)
        assertEquals(8.dp, config.verticalPadding)
    }

    @Test
    fun `phone short widget uses the same balanced style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 110.dp,
            height = 60.dp
        )

        assertEquals(SimpleWidgetLayoutStyle.BALANCED_SHORT, config.style)
        assertEquals(13.sp, config.displayNameFontSize)
        assertEquals(19.sp, config.valueFontSize)
        assertEquals(10.sp, config.secondaryFontSize)
        assertEquals(6.dp, config.verticalPadding)
    }

    @Test
    fun `horizontal phone widget uses balanced style despite tall launcher cells`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 150.dp,
            height = 110.dp,
            isTablet = false
        )

        assertEquals(SimpleWidgetLayoutStyle.BALANCED_SHORT, config.style)
        assertEquals(13.sp, config.displayNameFontSize)
        assertEquals(19.sp, config.valueFontSize)
        assertEquals(10.sp, config.secondaryFontSize)
    }

    @Test
    fun `tall widget keeps standard style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 220.dp
        )

        assertEquals(SimpleWidgetLayoutStyle.STANDARD, config.style)
        assertEquals(16.sp, config.displayNameFontSize)
    }

    @Test
    fun `AQI value spans superscript and subscript stack`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 110.dp,
            isTablet = true
        )

        assertEquals(
            20.sp,
            calculateAqiValueFontSize(config, showMeasurementDescription = true)
        )
        assertEquals(
            14.sp,
            calculateAqiValueFontSize(config, showMeasurementDescription = false)
        )

        val phoneConfig = SimpleWidgetLayoutConfig.fromSize(
            width = 110.dp,
            height = 60.dp
        )
        assertEquals(
            24.sp,
            calculateAqiValueFontSize(phoneConfig, showMeasurementDescription = true)
        )
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
            isAirQuality = false,
            primaryContentMandatory = true
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `short widget keeps mandatory content and hides measurement name`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 54.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 54.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 15.dp,
                secondary = 7.dp
            ),
            hasUnit = true,
            isAirQuality = false,
            primaryContentMandatory = true
        )

        assertTrue(visibility.showDisplayName)
        assertEquals(false, visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `timestamp takes priority over measurement name when only one optional row fits`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 54.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 54.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 15.dp,
                secondary = 7.dp
            ),
            hasUnit = true,
            isAirQuality = false,
            primaryContentMandatory = true
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showTimestamp)
        assertEquals(false, visibility.showMeasurementDescription)
    }

    @Test
    fun `mandatory content remains visible with very large text`() {
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
            isAirQuality = false,
            primaryContentMandatory = true
        )

        assertTrue(visibility.showDisplayName)
        assertEquals(false, visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `air quality progress is hidden before text rows are compressed`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp).copy(
            glowSize = 20.dp
        )

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 80.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 18.dp,
                secondary = 7.dp
            ),
            hasUnit = false,
            isAirQuality = true,
            primaryContentMandatory = true
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showMeasurementDescription)
        assertEquals(false, visibility.showAqiProgress)
        assertTrue(visibility.showTimestamp)
    }
}
