package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.widgets.ui.glance.WidgetTextMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetLayoutConfigTest {
    @Test
    fun `measurement unit top spacing separates the unit and measurement label`() {
        val value = WidgetTextMetrics(
            height = 20.dp,
            baseline = 16.dp,
            glyphTop = 2.dp,
            glyphBottom = 18.dp
        )
        val unit = WidgetTextMetrics(
            height = 10.dp,
            baseline = 8.dp,
            glyphTop = 1.dp,
            glyphBottom = 9.dp
        )
        val measurement = WidgetTextMetrics(
            height = 14.dp,
            baseline = 8.dp,
            glyphTop = 1.dp,
            glyphBottom = 13.dp
        )

        val baseLayout = calculateAqiTextLayout(value, unit, measurement, indicatorSize = 0.dp)
        val spacedLayout = calculateAqiTextLayout(
            value, unit, measurement, indicatorSize = 0.dp, measurementUnitTopSpacing = 1.dp
        )

        assertEquals(1.dp, baseLayout.unitTopPadding - spacedLayout.unitTopPadding)
    }

    @Test
    fun `short widget uses compact geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 40.dp)

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(8.dp, config.verticalPadding)
        assertEquals(14.sp, config.displayNameFontSize)
        assertEquals(20.sp, config.valueFontSize)
    }

    @Test
    fun `tall widget uses roomy geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 90.dp)

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(12.dp, config.verticalPadding)
        assertEquals(20.sp, config.displayNameFontSize)
    }

    @Test
    fun `medium widget uses compact sensor name`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp)

        assertEquals(16.sp, config.displayNameFontSize)
    }

    @Test
    fun `display zoom does not change selected layout geometry`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(
            height = 40.dp,
            zoomFactor = 2f
        )

        assertEquals(12.dp, config.horizontalPadding)
        assertEquals(8.dp, config.verticalPadding)
        assertEquals(14.sp, config.displayNameFontSize)
    }

    @Test
    fun `wide short widget has dedicated balanced style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 110.dp,
            isTablet = true
        )

        assertEquals(SimpleWidgetLayoutStyle.BALANCED_SHORT, config.style)
        assertEquals(12.sp, config.displayNameFontSize)
        assertEquals(18.sp, config.valueFontSize)
        assertEquals(10.sp, config.secondaryFontSize)
        assertEquals(8.dp, config.verticalPadding)
    }

    @Test
    fun `phone short widget uses the same balanced style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 110.dp,
            height = 60.dp
        )

        assertEquals(SimpleWidgetLayoutStyle.BALANCED_SHORT, config.style)
        assertEquals(16.sp, config.displayNameFontSize)
        assertEquals(24.sp, config.valueFontSize)
        assertEquals(12.sp, config.secondaryFontSize)
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
        assertEquals(16.sp, config.displayNameFontSize)
        assertEquals(24.sp, config.valueFontSize)
        assertEquals(12.sp, config.secondaryFontSize)
    }

    @Test
    fun `tall widget keeps standard style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 220.dp
        )

        assertEquals(SimpleWidgetLayoutStyle.STANDARD, config.style)
        assertEquals(20.sp, config.displayNameFontSize)
    }

    @Test
    fun `AQI value spans superscript and subscript stack`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 110.dp,
            isTablet = true
        )

        assertEquals(
            24.sp,
            calculateAqiValueFontSize(config)
        )

        val phoneConfig = SimpleWidgetLayoutConfig.fromSize(
            width = 110.dp,
            height = 60.dp
        )
        assertEquals(
            28.sp,
            calculateAqiValueFontSize(phoneConfig)
        )
    }

    @Test
    fun `simple widget typography stays unchanged when all rows fit`() {
        val config = SimpleWidgetLayoutConfig.fromSize(220.dp, 110.dp)

        assertEquals(config, fitSimpleWidgetTypography(config) { true })
    }

    @Test
    fun `simple widget typography scales every text size together without changing geometry`() {
        val config = SimpleWidgetLayoutConfig.fromSize(110.dp, 40.dp)
        val fitted = fitSimpleWidgetTypography(config) {
            it.valueFontSize.value <= config.valueFontSize.value / 2f
        }

        assertEquals(config.displayNameFontSize / 2, fitted.displayNameFontSize)
        assertEquals(config.valueFontSize / 2, fitted.valueFontSize)
        assertEquals(config.secondaryFontSize / 2, fitted.secondaryFontSize)
        assertEquals(
            config,
            fitted.copy(
                displayNameFontSize = config.displayNameFontSize,
                valueFontSize = config.valueFontSize,
                secondaryFontSize = config.secondaryFontSize
            )
        )
    }

    @Test
    fun `simple widget typography selects the largest fitting scale`() {
        val config = SimpleWidgetLayoutConfig.fromSize(110.dp, 40.dp)
        val maximumValueFontSize = config.valueFontSize.value * 0.37f
        val fitted = fitSimpleWidgetTypography(config) {
            it.valueFontSize.value <= maximumValueFontSize
        }

        assertTrue(fitted.valueFontSize.value <= maximumValueFontSize)
        assertEquals(maximumValueFontSize, fitted.valueFontSize.value, 0.01f)
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
    fun `short widget keeps mandatory content and measurement name`() {
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
        assertTrue(visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `timestamp and measurement name remain visible when only one optional row fits`() {
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
        assertTrue(visibility.showMeasurementDescription)
    }

    @Test
    fun `mandatory content and measurement name remain visible with very large text`() {
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
        assertTrue(visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `air quality keeps all rows visible`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 80.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 32.dp,
                secondary = 7.dp
            ),
            hasUnit = false,
            isAirQuality = true,
            primaryContentMandatory = true
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `air quality never hides rows when the text needs to be scaled down`() {
        val config = SimpleWidgetLayoutConfig.fromHeight(height = 80.dp)

        val visibility = calculateSimpleWidgetContentVisibility(
            widgetHeight = 80.dp,
            config = config,
            textHeights = SimpleWidgetTextHeights(
                displayName = 10.dp,
                value = 40.dp,
                secondary = 7.dp
            ),
            hasUnit = false,
            isAirQuality = true,
            primaryContentMandatory = false
        )

        assertTrue(visibility.showDisplayName)
        assertTrue(visibility.showMeasurementDescription)
        assertTrue(visibility.showTimestamp)
    }

    @Test
    fun `AQI labels align to the numeric cap line and baseline across densities and font sizes`() {
        for (density in listOf(1f, 1.5f, 2f, 3.5f)) {
            for (fontScale in listOf(0.85f, 1f, 1.3f, 2f)) {
                val valueScale = 1f + (fontScale - 1f) * 0.7f
                val value = WidgetTextMetrics(
                    height = (46f * valueScale / density).dp,
                    baseline = (38f * valueScale / density).dp,
                    glyphTop = (10f * valueScale / density).dp,
                    glyphBottom = (39f * valueScale / density).dp
                )
                val unit = WidgetTextMetrics(
                    height = (16f * fontScale / density).dp,
                    baseline = (13f * fontScale / density).dp,
                    glyphTop = (4f * fontScale / density).dp,
                    glyphBottom = (14f * fontScale / density).dp
                )
                val measurement = unit.copy(
                    baseline = (12f * fontScale / density).dp,
                    glyphTop = (3f * fontScale / density).dp
                )
                val indicatorSize = 8.dp
                val layout = calculateAqiTextLayout(value, unit, measurement, indicatorSize)
                val unitTextTop = (maxOf(unit.height, indicatorSize) - unit.height) / 2

                assertEquals(
                    (layout.valueTopPadding + value.glyphTop).value,
                    (layout.unitTopPadding + unitTextTop + unit.glyphTop).value,
                    0.001f
                )
                assertEquals(
                    (layout.valueTopPadding + value.baseline).value,
                    (layout.measurementTopPadding + measurement.baseline).value,
                    0.001f
                )
                assertTrue(layout.valueTopPadding >= 0.dp)
                assertTrue(layout.unitTopPadding >= 0.dp)
                assertTrue(layout.measurementTopPadding >= 0.dp)
                assertTrue(layout.height >= layout.valueTopPadding + value.height)
                assertTrue(layout.height >= layout.unitTopPadding + maxOf(unit.height, indicatorSize))
                assertTrue(layout.height >= layout.measurementTopPadding + measurement.height)
            }
        }
    }

    @Test
    fun `AQI minimum value height leaves space between the labels`() {
        val unit = WidgetTextMetrics(16.dp, 13.dp, 4.dp, 14.dp)
        val measurement = WidgetTextMetrics(16.dp, 12.dp, 3.dp, 15.dp)
        val minimumHeight = calculateAqiMinimumValueHeight(
            unit, measurement, indicatorSize = 8.dp, spacing = 2.dp
        )
        val value = WidgetTextMetrics(
            height = minimumHeight + 12.dp,
            baseline = minimumHeight + 8.dp,
            glyphTop = 8.dp,
            glyphBottom = minimumHeight + 9.dp
        )
        val layout = calculateAqiTextLayout(value, unit, measurement, indicatorSize = 8.dp)

        assertEquals(21.dp, minimumHeight)
        assertEquals(
            2.dp,
            layout.measurementTopPadding + measurement.glyphTop -
                layout.unitTopPadding - unit.glyphBottom
        )
    }

    @Test
    fun `AQI description descenders contribute to row height`() {
        val value = WidgetTextMetrics(30.dp, 29.dp, 6.dp, 29.dp)
        val unit = WidgetTextMetrics(14.dp, 11.dp, 3.dp, 12.dp)
        val measurement = WidgetTextMetrics(16.dp, 11.dp, 3.dp, 15.dp)

        val layout = calculateAqiTextLayout(value, unit, measurement, indicatorSize = 8.dp)

        assertEquals(34.dp, layout.height)
        assertEquals(18.dp, layout.measurementTopPadding)
    }

    @Test
    fun `AQI retains top alignment when the indicator is taller than text`() {
        val value = WidgetTextMetrics(12.dp, 10.dp, 1.dp, 11.dp)
        val unit = WidgetTextMetrics(6.dp, 5.dp, 2.dp, 6.dp)
        val measurement = WidgetTextMetrics(6.dp, 5.dp, 2.dp, 6.dp)

        val layout = calculateAqiTextLayout(value, unit, measurement, indicatorSize = 10.dp)

        assertEquals(3.dp, layout.valueTopPadding)
        assertEquals(0.dp, layout.unitTopPadding)
        assertEquals(15.dp, layout.height)
    }
}
