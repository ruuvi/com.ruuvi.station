package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.widgets.ui.glance.WidgetTextMetrics
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetLayoutConfigTest {
    @Test
    fun `initial cell size remains the typography size after enlarging the widget`() {
        val initialSize = DpSize(180.dp, 110.dp)

        assertEquals(
            initialSize,
            simpleWidgetTypographySize(
                currentSize = DpSize(360.dp, 220.dp),
                observedSize = initialSize
            )
        )
    }

    @Test
    fun `initial cell size remains the typography size after reducing the widget`() {
        val initialSize = DpSize(180.dp, 110.dp)

        assertEquals(
            initialSize,
            simpleWidgetTypographySize(
                currentSize = DpSize(82.dp, 110.dp),
                observedSize = initialSize
            )
        )
    }

    @Test
    fun `reducing widget width keeps the initial value bitmap width budget`() {
        val initialSize = DpSize(180.dp, 110.dp)

        assertEquals(
            156.dp,
            simpleWidgetTypographyAvailableWidth(
                currentSize = DpSize(82.dp, 110.dp),
                observedSize = initialSize,
                horizontalPadding = 12.dp
            )
        )
    }

    @Test
    fun `reading row never becomes narrower than the initial cell`() {
        val initialSize = DpSize(180.dp, 110.dp)

        assertEquals(
            156.dp,
            simpleWidgetReadingAvailableWidth(
                currentSize = DpSize(82.dp, 110.dp),
                observedSize = initialSize,
                horizontalPadding = 12.dp
            )
        )
    }

    @Test
    fun `reading row expands to reveal truncated labels without changing typography`() {
        val initialSize = DpSize(180.dp, 110.dp)

        assertEquals(
            336.dp,
            simpleWidgetReadingAvailableWidth(
                currentSize = DpSize(360.dp, 110.dp),
                observedSize = initialSize,
                horizontalPadding = 12.dp
            )
        )
        assertEquals(
            156.dp,
            simpleWidgetTypographyAvailableWidth(
                currentSize = DpSize(360.dp, 110.dp),
                observedSize = initialSize,
                horizontalPadding = 12.dp
            )
        )
    }

    @Test
    fun `current size is used until a valid initial cell size is observed`() {
        val currentSize = DpSize(180.dp, 110.dp)

        assertEquals(currentSize, simpleWidgetTypographySize(currentSize, null))
        assertEquals(null, observedSimpleWidgetTypographySize(width = 180, height = 0))
        assertEquals(
            currentSize,
            observedSimpleWidgetTypographySize(width = 180, height = 110)
        )
    }

    @Test
    fun `fitted value is reduced by eight sp`() {
        for (size in listOf(10.sp, 12.sp, 24.sp, 60.sp)) {
            assertEquals((size.value - 8f).sp, reduceSimpleWidgetValueFontSize(size))
        }
    }

    @Test
    fun `tiny value sizes are not increased or reduced to zero`() {
        assertEquals(2.sp, reduceSimpleWidgetValueFontSize(9.sp))
        assertEquals(2.sp, reduceSimpleWidgetValueFontSize(7.sp))
        assertEquals(2.sp, reduceSimpleWidgetValueFontSize(4.sp))
        assertEquals(2.sp, reduceSimpleWidgetValueFontSize(2.sp))
        assertEquals(1.sp, reduceSimpleWidgetValueFontSize(1.sp))
    }

    @Test
    fun `label text grows to the largest size that fits its half`() {
        val fontSize = fitSimpleWidgetLabelFontSize(maximumFontSize = 60.sp) {
            it.value * 1.25f <= 30f
        }

        assertTrue(fontSize.value * 1.25f <= 30f)
        assertEquals(24f, fontSize.value, 0.02f)
    }

    @Test(expected = IllegalStateException::class)
    fun `label fitting reports when no size fits`() {
        fitSimpleWidgetLabelFontSize(maximumFontSize = 60.sp) { false }
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
    fun `widening tablet widget keeps height based typography`() {
        val narrowConfig = SimpleWidgetLayoutConfig.fromSize(
            width = 160.dp,
            height = 110.dp,
            isTablet = true
        )
        val wideConfig = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 110.dp,
            isTablet = true
        )

        assertEquals(SimpleWidgetLayoutStyle.STANDARD, wideConfig.style)
        assertEquals(narrowConfig, wideConfig)
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
        assertEquals(12.sp, config.timestampFontSize)
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
        assertEquals(12.sp, config.timestampFontSize)
    }

    @Test
    fun `tall widget keeps standard style`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 220.dp
        )

        assertEquals(SimpleWidgetLayoutStyle.STANDARD, config.style)
        assertEquals(24.sp, config.displayNameFontSize)
    }

    @Test
    fun `AQI value spans superscript and subscript stack`() {
        val config = SimpleWidgetLayoutConfig.fromSize(
            width = 220.dp,
            height = 110.dp,
            isTablet = true
        )

        assertEquals(
            32.sp,
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
        assertEquals(config.timestampFontSize / 2, fitted.timestampFontSize)
        assertEquals(config.measurementNameFontSize / 2, fitted.measurementNameFontSize)
        assertEquals(
            config,
            fitted.copy(
                displayNameFontSize = config.displayNameFontSize,
                valueFontSize = config.valueFontSize,
                secondaryFontSize = config.secondaryFontSize,
                timestampFontSize = config.timestampFontSize,
                measurementNameFontSize = config.measurementNameFontSize
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
    fun `value grows to fill the height between the title and timestamp`() {
        val config = SimpleWidgetLayoutConfig.fromSize(220.dp, 110.dp)
        val availableHeight = 110f - config.verticalPadding.value * 2 - 5f
        val fitted = fitSimpleWidgetTypography(config, maximumValueFontSize = 110.sp) {
            it.displayNameFontSize.value + it.valueFontSize.value +
                it.timestampFontSize.value <= availableHeight
        }

        assertTrue(fitted.valueFontSize > config.valueFontSize)
        assertEquals(
            availableHeight - config.displayNameFontSize.value - config.timestampFontSize.value,
            fitted.valueFontSize.value,
            0.03f
        )
        assertEquals(config, fitted.copy(valueFontSize = config.valueFontSize))
    }

    @Test
    fun `value growth stops at the available width without shrinking the title or timestamp`() {
        val config = SimpleWidgetLayoutConfig.fromSize(220.dp, 110.dp)
        val fitted = fitSimpleWidgetTypography(config, maximumValueFontSize = 110.sp) {
            it.valueFontSize.value <= 35f
        }

        assertEquals(35f, fitted.valueFontSize.value, 0.03f)
        assertEquals(config, fitted.copy(valueFontSize = config.valueFontSize))
    }

    @Test
    fun `value still fits when all typography must shrink in a small widget`() {
        val config = SimpleWidgetLayoutConfig.fromSize(110.dp, 40.dp)
        val fitted = fitSimpleWidgetTypography(config, maximumValueFontSize = 40.sp) {
            it.displayNameFontSize.value + it.valueFontSize.value +
                it.timestampFontSize.value <= 20f
        }

        assertTrue(fitted.displayNameFontSize < config.displayNameFontSize)
        assertTrue(fitted.timestampFontSize < config.timestampFontSize)
        val totalHeight = fitted.displayNameFontSize.value + fitted.valueFontSize.value +
            fitted.timestampFontSize.value
        assertTrue(totalHeight <= 20f)
        assertEquals(20f, totalHeight, 0.03f)
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
    fun `measurement row reserves only refresh overlap inside content`() {
        val refreshOverlap = calculateRefreshOverlap(
            contentEndPadding = 12.dp,
            refreshTouchTargetSize = 40.dp
        )

        assertEquals(28.dp, refreshOverlap)
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
    fun `unit and measurement each receive half the value height`() {
        val heights = calculateSimpleWidgetLabelHeights(valueHeight = 40.dp, density = 1f)

        assertEquals(20.dp, heights.unit)
        assertEquals(20.dp, heights.measurement)
    }

    @Test
    fun `odd pixel heights keep the two label rows within the value height`() {
        val heights = calculateSimpleWidgetLabelHeights(valueHeight = 41.dp, density = 1f)

        assertEquals(21.dp, heights.unit)
        assertEquals(20.dp, heights.measurement)
        assertEquals(41.dp, heights.unit + heights.measurement)
    }

    @Test
    fun `label halves use every pixel without overflow across densities`() {
        for (density in listOf(1f, 1.5f, 2f, 2.625f, 3.5f)) {
            for (heightPx in listOf(2, 3, 12, 45, 80, 301)) {
                val heights = calculateSimpleWidgetLabelHeights((heightPx / density).dp, density)
                val unitHeightPx = (heights.unit.value * density).roundToInt()
                val measurementHeightPx = (heights.measurement.value * density).roundToInt()

                assertEquals(heightPx, unitHeightPx + measurementHeightPx)
                assertTrue(abs(unitHeightPx - measurementHeightPx) <= 1)
                assertTrue(unitHeightPx > 0)
                assertTrue(measurementHeightPx > 0)
            }
        }
    }

    @Test
    fun `unit tops and measurement baselines align while preserving native descenders`() {
        val value = WidgetTextMetrics(80.dp, 78.dp, 2.dp, 78.dp)
        val unit = WidgetTextMetrics(40.dp, 36.dp, 1.dp, 37.dp)
        val measurement = WidgetTextMetrics(40.dp, 30.dp, 8.dp, 35.dp)
        val layout = calculateSimpleWidgetTextLayout(value, unit, measurement, density = 1f)

        assertEquals(1.dp, layout.unitTopPadding)
        assertEquals(value.glyphTop, layout.unitTopPadding + unit.glyphTop)
        assertEquals(48.dp, layout.measurementTopPadding)
        assertEquals(value.baseline, layout.measurementTopPadding + measurement.baseline)
        assertEquals(88.dp, layout.height)
        assertEquals(layout.height, layout.measurementTopPadding + measurement.height)
    }

    @Test
    fun `numeric descenders can contain the baseline aligned measurement`() {
        val value = WidgetTextMetrics(50.dp, 40.dp, 1.dp, 49.dp)
        val unit = WidgetTextMetrics(24.dp, 20.dp, 1.dp, 23.dp)
        val measurement = WidgetTextMetrics(24.dp, 18.dp, 5.dp, 22.dp)
        val layout = calculateSimpleWidgetTextLayout(value, unit, measurement, density = 1f)

        assertEquals(0.dp, layout.unitTopPadding)
        assertEquals(22.dp, layout.measurementTopPadding)
        assertEquals(value.baseline, layout.measurementTopPadding + measurement.baseline)
        assertEquals(value.height, layout.height)
    }

    @Test
    fun `baseline alignment uses exact physical pixel positions across densities`() {
        for (density in listOf(1f, 1.5f, 2f, 2.625f, 3.5f)) {
            fun metrics(height: Int, baseline: Int, top: Int, bottom: Int) = WidgetTextMetrics(
                height = (height / density).dp,
                baseline = (baseline / density).dp,
                glyphTop = (top / density).dp,
                glyphBottom = (bottom / density).dp
            )
            val layout = calculateSimpleWidgetTextLayout(
                valueMetrics = metrics(80, 78, 2, 78),
                unitMetrics = metrics(40, 36, 1, 37),
                measurementMetrics = metrics(40, 30, 8, 35),
                density = density
            )

            assertEquals(1, (layout.unitTopPadding.value * density).roundToInt())
            assertEquals(48, (layout.measurementTopPadding.value * density).roundToInt())
            assertEquals(88, (layout.height.value * density).roundToInt())
        }
    }
}
