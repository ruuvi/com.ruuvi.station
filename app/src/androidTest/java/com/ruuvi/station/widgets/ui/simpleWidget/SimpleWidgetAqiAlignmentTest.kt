package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.text.style.TextAppearanceSpan
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.descendants
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.R as GlanceR
import androidx.test.InstrumentationRegistry
import com.ruuvi.station.R
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.glance.GlanceFontUtils
import com.ruuvi.station.widgets.ui.glance.getZoomFactor
import com.ruuvi.station.widgets.ui.glance.toWidgetSp
import java.util.concurrent.FutureTask
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetAqiAlignmentTest {
    @Test
    fun boldFontMetricsMatchGlanceDeviceDefaultTextAppearance() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assertion = FutureTask {
            val context = ContextThemeWrapper(
                instrumentation.targetContext,
                GlanceR.style.Glance_AppWidget_Theme
            )
            val density = context.resources.displayMetrics.density
            for (fontSize in listOf(8.sp, 12.sp, 18.sp)) {
                val text = SpannableString("/100").apply {
                    setSpan(
                        TextAppearanceSpan(context, GlanceR.style.Glance_AppWidget_TextAppearance_Bold),
                        0,
                        length,
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
                val nativeText = TextView(context).apply {
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize.value)
                    maxLines = 1
                    this.text = text
                    measure(0, 0)
                }
                val metrics = GlanceFontUtils.measureSystemFontMetrics(
                    context, text.toString(), fontSize, bold = true
                )
                val bounds = textBounds(nativeText)

                assertEquals(nativeText.baseline.toFloat(), metrics.baseline.value * density, 0.001f)
                assertEquals(
                    (nativeText.baseline + bounds.top).toFloat(),
                    metrics.glyphTop.value * density,
                    0.001f
                )
                assertEquals(nativeText.measuredHeight.toFloat(), metrics.height.value * density, 0.001f)
                assertEquals(nativeText.measuredWidth.toFloat(), metrics.width.value * density, 0.001f)
            }
        }
        instrumentation.runOnMainSync(assertion)
        assertion.get()
    }

    @Test
    fun labelsAlignWithBitmapValueAcrossFontScalesAndDeviceSizes() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseContext = instrumentation.targetContext
        val readings = listOf(
            Reading("100", "/100", "Air quality", WidgetType.AIR_QUALITY),
            Reading("-21.5", "\u00b0C", "Temperature", WidgetType.TEMPERATURE),
            Reading("-21,5", "\u00b0C", "Temperature", WidgetType.TEMPERATURE),
            Reading("1234", "ppm", "Carbon dioxide", WidgetType.CO2)
        )

        for (isTablet in listOf(false, true)) {
            for (densityDpi in listOf(160, 420)) {
                for (fontScale in listOf(0.85f, 1f, 1.3f, 2f)) {
                    val context = baseContext.createConfigurationContext(
                        Configuration(baseContext.resources.configuration).apply {
                            this.densityDpi = densityDpi
                            this.fontScale = fontScale
                            smallestScreenWidthDp = if (isTablet) 600 else 360
                        }
                    )
                    for (size in listOf(
                        DpSize(40.dp, 40.dp),
                        DpSize(110.dp, 40.dp),
                        DpSize(110.dp, 60.dp),
                        DpSize(82.dp, 45.dp),
                        DpSize(188.dp, 96.dp),
                        DpSize(200.dp, 220.dp),
                        DpSize(400.dp, 220.dp)
                    )) {
                        for (reading in readings) {
                            val remoteViews = try {
                                SimpleWidgetGlanceWidget.compose(
                                    context = context,
                                    size = size,
                                    state = reading.state()
                                )
                            } catch (error: IllegalStateException) {
                                throw AssertionError(
                                    "Cannot render $reading at $size, density=$densityDpi, " +
                                        "fontScale=$fontScale, tablet=$isTablet",
                                    error
                                )
                            }
                            val assertion = FutureTask {
                                assertReadingLayout(context, remoteViews, size, reading)
                            }
                            instrumentation.runOnMainSync(assertion)
                            assertion.get()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun labelsGrowAndShrinkToFillTheirHalfOfTheValueHeight() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assertion = FutureTask {
            val context = instrumentation.targetContext
            val base = SimpleWidgetLayoutConfig.fromSize(188.dp, 96.dp)
            for (config in listOf(
                base.copy(valueFontSize = 60.sp, secondaryFontSize = 8.sp),
                base.copy(valueFontSize = 12.sp, secondaryFontSize = 24.sp),
                base.copy(valueFontSize = 0.1.sp, secondaryFontSize = 24.sp)
            )) {
                val layout = measureAqiLayout(context, config, "Air quality")
                val originalLabelSize = config.secondaryFontSize.toWidgetSp(context).value
                val originalMeasurementSize = config.measurementNameFontSize.toWidgetSp(context).value
                if (config.valueFontSize > config.secondaryFontSize) {
                    assertTrue(layout.labelFontSize.value > originalLabelSize)
                    assertTrue(layout.measurementNameFontSize.value > originalMeasurementSize)
                } else {
                    assertTrue(layout.labelFontSize.value < originalLabelSize)
                    assertTrue(layout.measurementNameFontSize.value < originalMeasurementSize)
                }
                assertLabelsFillHalfHeights(context, layout, "/100", "Air quality")
            }
        }
        instrumentation.runOnMainSync(assertion)
        assertion.get()
    }

    @Test
    fun numericFontFittingSupportsSmallerAndLargerCapHeights() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val smaller = GlanceFontUtils.fitCustomFontSizeToBaselineHeight(
            context, 60.sp, R.font.oswald_bold, 10.dp
        )
        val larger = GlanceFontUtils.fitCustomFontSizeToBaselineHeight(
            context, 4.sp, R.font.oswald_bold, 30.dp
        )

        assertTrue(smaller < 60.sp)
        assertTrue(larger > 4.sp)
        for ((fontSize, target) in listOf(smaller to 10.dp, larger to 30.dp)) {
            val metrics = GlanceFontUtils.measureCustomFontMetrics(context, fontSize, R.font.oswald_bold)
            val density = context.resources.displayMetrics.density
            assertEquals(
                target.value * density,
                (metrics.baseline - metrics.glyphTop).value * density,
                1f
            )
        }
    }

    @Test
    fun unitlessReadingKeepsMeasurementOnTheValueBaseline() = runBlocking<Unit> {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val size = DpSize(188.dp, 96.dp)
        val reading = Reading("42", "", "Movement counter", WidgetType.MOVEMENT)
        val remoteViews = SimpleWidgetGlanceWidget.compose(
            context = context, size = size, state = reading.state()
        )
        val assertion = FutureTask { assertReadingLayout(context, remoteViews, size, reading) }
        instrumentation.runOnMainSync(assertion)
        assertion.get()
    }

    @Test
    fun wholeAndDecimalReadingsKeepTheSameFontSizeWhenHeightLimited() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assertion = FutureTask {
            val baseContext = instrumentation.targetContext
            for (isTablet in listOf(false, true)) {
                for (densityDpi in listOf(160, 450)) {
                    for (fontScale in listOf(1f, 2f)) {
                        val context = baseContext.createConfigurationContext(
                            Configuration(baseContext.resources.configuration).apply {
                                this.densityDpi = densityDpi
                                this.fontScale = fontScale
                                smallestScreenWidthDp = if (isTablet) 600 else 360
                            }
                        )
                        for (size in listOf(DpSize(188.dp, 96.dp), DpSize(400.dp, 96.dp))) {
                            val baseConfig = SimpleWidgetLayoutConfig.fromSize(size.width, size.height, isTablet)
                            val layouts = listOf("28", "28.5", "28,5").map { value ->
                                val config = fitSimpleWidgetConfig(
                                    context, baseConfig, size, value, "\u00b0C", "Temperature",
                                    isAirQuality = false
                                )
                                value to measureMeasurementLayout(
                                    context, config, "\u00b0C", "Temperature", value
                                )
                            }
                            val expected = layouts.first().second
                            for ((value, layout) in layouts) {
                                val scenario = "$value / $size / tablet=$isTablet / density=$densityDpi / " +
                                    "fontScale=$fontScale"
                                assertEquals("$scenario: number font", expected.valueFontSize, layout.valueFontSize)
                                assertEquals("$scenario: unit font", expected.labelFontSize, layout.labelFontSize)
                                assertEquals(
                                    "$scenario: measurement font",
                                    expected.measurementNameFontSize,
                                    layout.measurementNameFontSize
                                )
                                assertEquals("$scenario: label budgets", expected.labelHeights, layout.labelHeights)
                                assertEquals("$scenario: baselines", expected.textLayout, layout.textLayout)
                            }
                        }
                    }
                }
            }
        }
        instrumentation.runOnMainSync(assertion)
        assertion.get()
    }

    @Test
    fun renderedWholeAndDecimalReadingsKeepTheSameNumberHeight() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val size = DpSize(188.dp, 96.dp)
        val heights = listOf("28", "28.5", "28,5").map { text ->
            val reading = Reading(text, "\u00b0C", "Temperature", WidgetType.TEMPERATURE)
            val remoteViews = SimpleWidgetGlanceWidget.compose(
                context = context, size = size, state = reading.state()
            )
            val assertion = FutureTask {
                assertReadingLayout(context, remoteViews, size, reading)
                val root = applyWidget(context, remoteViews, size)
                val value = root.descendants.filterIsInstance<ImageView>()
                    .single { it.contentDescription == text }
                value.height
            }
            instrumentation.runOnMainSync(assertion)
            assertion.get()
        }

        assertEquals("Formatting must not change the rendered number height", 1, heights.distinct().size)
    }

    @Test
    fun numericBitmapsKeepStableHeightAndBaselineAcrossFormats() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        for (fontSize in listOf(8.sp, 18.sp, 48.sp)) {
            val reference = GlanceFontUtils.measureCustomFontMetrics(
                context, fontSize, R.font.oswald_bold, "28,5",
                trimVerticalPadding = true, stableNumericBounds = true
            )
            for (embeddedColor in listOf(null, Color.WHITE)) {
                for (text in listOf("28", "28.5", "28,5", "-28", "-28.5", "-28,5", "100")) {
                    val metrics = GlanceFontUtils.measureCustomFontMetrics(
                        context, fontSize, R.font.oswald_bold, text,
                        trimVerticalPadding = true, stableNumericBounds = true
                    )
                    val bitmap = GlanceFontUtils.createFontBitmap(
                        context, text, fontSize, R.font.oswald_bold, maxWidth = 1000,
                        embeddedColor = embeddedColor, trimVerticalPadding = true, stableNumericBounds = true
                    )
                    val scenario = "$text / $fontSize / embeddedColor=$embeddedColor"

                    assertEquals("$scenario: stable height", reference.height, metrics.height)
                    assertEquals("$scenario: stable baseline", reference.baseline, metrics.baseline)
                    assertEquals(
                        "$scenario: measured height",
                        metrics.height.value * density, bitmap.height.toFloat(), 0.001f
                    )
                    assertEquals(
                        "$scenario: measured width",
                        metrics.width.value * density, bitmap.width.toFloat(), 0.001f
                    )
                }
            }
        }
    }

    @Test
    fun valueKeepsSevenDpTitleGapWithoutChangingItsSizingBudget() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val baseContext = instrumentation.targetContext
        val readings = listOf(
            Reading("21.5", "\u00b0C", "Temperature", WidgetType.TEMPERATURE),
            Reading("21,5", "\u00b0C", "Temperature", WidgetType.TEMPERATURE),
            Reading("100", "/100", "Air quality", WidgetType.AIR_QUALITY)
        )
        for (isTablet in listOf(false, true)) {
            for (densityDpi in listOf(160, 420)) {
                val context = baseContext.createConfigurationContext(
                    Configuration(baseContext.resources.configuration).apply {
                        this.densityDpi = densityDpi
                        fontScale = 1f
                        smallestScreenWidthDp = if (isTablet) 600 else 360
                    }
                )
                for (height in listOf(96.dp, 110.dp, 120.dp)) {
                    val size = DpSize(400.dp, height)
                    for (reading in readings) {
                        val remoteViews = SimpleWidgetGlanceWidget.compose(
                            context = context, size = size, state = reading.state()
                        )
                        val assertion = FutureTask {
                            val root = applyWidget(context, remoteViews, size)
                            val value = root.descendants.filterIsInstance<ImageView>()
                                .single { it.contentDescription == reading.value }
                            val title = root.descendants.filterIsInstance<TextView>()
                                .single { it.text.toString() == "Living room" }
                            val timestamp = root.descendants.filterIsInstance<TextView>()
                                .single { it.text.toString() == "12:34" }
                            val config = fitSimpleWidgetConfig(
                                context,
                                SimpleWidgetLayoutConfig.fromSize(size.width, size.height, isTablet),
                                size, reading.value, reading.unit, reading.measurement, reading.isAqi
                            )
                            val layout = if (reading.isAqi) {
                                measureAqiLayout(context, config, reading.measurement, reading.value)
                            } else {
                                measureMeasurementLayout(
                                    context, config, reading.unit, reading.measurement, reading.value
                                )
                            }
                            val fullSizeConfig = config.copy(
                                valueFontSize = (
                                    (layout.valueFontSize.value + 8f) * getZoomFactor(context)
                                ).sp
                            )
                            val fullSizeLayout = if (reading.isAqi) {
                                measureAqiLayout(context, fullSizeConfig, reading.measurement, reading.value)
                            } else {
                                measureMeasurementLayout(
                                    context, fullSizeConfig, reading.unit, reading.measurement, reading.value
                                )
                            }
                            val density = context.resources.displayMetrics.density
                            val titleSpacing = (7f * density).roundToInt()
                            val sizingTitleSpacing = (5f * density).roundToInt()
                            val titleBottom = top(root, title) + title.height
                            val readingTop = top(root, value)
                            val availableHeight = top(root, timestamp) - titleBottom - sizingTitleSpacing
                            val fullSizeValueMetrics = GlanceFontUtils.measureCustomFontMetrics(
                                context, fullSizeLayout.valueFontSize, R.font.oswald_bold,
                                reading.value, trimVerticalPadding = true, stableNumericBounds = true
                            )
                            val requiredHeight = maxOf(
                                ceil(fullSizeValueMetrics.height.value * density),
                                ceil(layout.textLayout.height.value * density)
                            )
                            val unusedHeight = availableHeight - requiredHeight
                            val numericLine = GlanceFontUtils.measureCustomFontMetrics(
                                context, layout.valueFontSize, R.font.oswald_bold, "0123456789,.",
                                trimVerticalPadding = true, stableNumericBounds = true
                            )
                            val numericLineHeight = (numericLine.glyphBottom - numericLine.glyphTop).value * density
                            val scenario = "$reading / $size / tablet=$isTablet / density=$densityDpi"

                            assertEquals(
                                "$scenario: use the seven dp gap below the title",
                                titleSpacing.toFloat(), readingTop - titleBottom, 1f
                            )
                            assertTrue(
                                "$scenario: preserve the original value sizing budget ($unusedHeight)",
                                unusedHeight in 0f..maxOf(3f, 2f * density)
                            )
                            assertEquals(
                                layout.valueFontSize.value,
                                reduceSimpleWidgetValueFontSize(fullSizeLayout.valueFontSize).value,
                                0.001f
                            )
                            assertTrue(
                                "$scenario: the displayed number must be smaller than its maximum fitting size",
                                layout.textLayout.height < fullSizeLayout.textLayout.height
                            )
                            assertEquals(
                                "$scenario: numeric bounds add only antialiasing padding",
                                2f,
                                value.height - numericLineHeight,
                                1f
                            )
                            assertReadingLayout(context, remoteViews, size, reading)
                        }
                        instrumentation.runOnMainSync(assertion)
                        assertion.get()
                    }
                }
            }
        }
    }

    @Test
    fun trimmedValuesKeepDecimalsSignsAndPlaceholderGlyphs() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        for (fontSize in listOf(18.sp, 48.sp)) {
            for (text in listOf("-21.5", "21,5", "\u0662\u0661\u066b\u0665", "-", "100")) {
                val original = GlanceFontUtils.createFontBitmap(
                    context, text, fontSize, R.font.oswald_bold, maxWidth = 1000
                )
                val trimmed = GlanceFontUtils.createFontBitmap(
                    context, text, fontSize, R.font.oswald_bold, maxWidth = 1000,
                    trimVerticalPadding = true, stableNumericBounds = true
                )
                val metrics = GlanceFontUtils.measureCustomFontMetrics(
                    context, fontSize, R.font.oswald_bold, text,
                    trimVerticalPadding = true, stableNumericBounds = true
                )
                val ink = inkBounds(trimmed)

                assertTrue("$text: trim the font's unused height", trimmed.height < original.height)
                assertEquals(trimmed.height.toFloat(), metrics.height.value * density, 0.001f)
                assertEquals(trimmed.width.toFloat(), metrics.width.value * density, 0.001f)
                assertEquals(inkBounds(original).height().toFloat(), ink.height().toFloat(), 2f)
                assertTrue("$text: retain top antialiasing", ink.top > 0)
                assertTrue("$text: retain bottom antialiasing", ink.bottom < trimmed.height)
            }
        }
    }

    @Test
    fun tinyGlyphMetricsMatchMinimumBitmapSizes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val density = context.resources.displayMetrics.density
        for (fontSize in listOf(0.1.sp, 1.sp, 2.sp)) {
            for ((text, fontResId) in listOf(
                "-" to R.font.oswald_bold,
                "100" to R.font.oswald_bold,
                "ppm" to R.font.oswald_regular
            )) {
                val bitmap = GlanceFontUtils.createFontBitmap(
                    context, text, fontSize, fontResId, maxWidth = 100, trimVerticalPadding = true,
                    stableNumericBounds = fontResId == R.font.oswald_bold
                )
                val metrics = GlanceFontUtils.measureCustomFontMetrics(
                    context, fontSize, fontResId, text, trimVerticalPadding = true,
                    stableNumericBounds = fontResId == R.font.oswald_bold
                )

                assertEquals(bitmap.width.toFloat(), metrics.width.value * density, 0.001f)
                assertEquals(bitmap.height.toFloat(), metrics.height.value * density, 0.001f)
            }
        }
    }

    @Test
    fun largeValuesAndUnitsFitRuntimeAndPreviewBitmapBudgets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assertion = FutureTask {
            val baseContext = instrumentation.targetContext
            for (densityDpi in listOf(160, 420)) {
                val context = baseContext.createConfigurationContext(
                    Configuration(baseContext.resources.configuration).apply {
                        this.densityDpi = densityDpi
                        fontScale = 1f
                    }
                )
                val density = context.resources.displayMetrics.density
                val size = DpSize(400.dp, 220.dp)
                for (embeddedColor in listOf(null, Color.WHITE)) {
                    for ((valueText, unitText, name) in listOf(
                        Triple("-1234,56", "ppm", "Carbon dioxide"),
                        Triple("1", "\u00b5g/m\u00b3", "Fine particles")
                    )) {
                        val config = fitSimpleWidgetConfig(
                            context, SimpleWidgetLayoutConfig.fromSize(size.width, size.height),
                            size, valueText, unitText, name, isAirQuality = false,
                            embeddedValueColor = embeddedColor
                        )
                        val layout = measureMeasurementLayout(context, config, unitText, name, valueText)
                        for ((text, fontSize, fontResId) in listOf(
                            Triple(valueText, layout.valueFontSize, R.font.oswald_bold),
                            Triple(unitText, layout.labelFontSize, R.font.oswald_regular)
                        )) {
                            val width = (GlanceFontUtils.measureCustomFontWidth(
                                context, text, fontSize, fontResId
                            ).value * density).roundToInt()
                            val bitmap = GlanceFontUtils.createFontBitmap(
                                context, text, fontSize, fontResId, width,
                                embeddedColor = embeddedColor, trimVerticalPadding = true,
                                stableNumericBounds = fontResId == R.font.oswald_bold
                            )

                            assertEquals("The full $text must fit without ellipsizing", width, bitmap.width)
                            assertTrue(bitmap.allocationByteCount <= GlanceFontUtils.MAX_BITMAP_BYTES)
                        }
                    }
                }
            }
        }
        instrumentation.runOnMainSync(assertion)
        assertion.get()
    }

    private fun assertReadingLayout(
        context: Context,
        remoteViews: RemoteViews,
        size: DpSize,
        reading: Reading
    ) {
        val density = context.resources.displayMetrics.density
        val root = applyWidget(context, remoteViews, size)
        val value = root.descendants.filterIsInstance<ImageView>()
            .single { it.contentDescription == reading.value }
        val unit = if (reading.unit.isBlank()) null else {
            root.descendants.filterIsInstance<ImageView>()
                .single { it.contentDescription == reading.unit }
        }
        val measurement = root.descendants.filterIsInstance<TextView>()
            .single { it.text.toString() == reading.measurement }
        val displayName = root.descendants.filterIsInstance<TextView>()
            .single { it.text.toString() == "Living room" }
        val timestamp = root.descendants.filterIsInstance<TextView>()
            .single { it.text.toString() == "12:34" }
        val baseConfig = SimpleWidgetLayoutConfig.fromSize(
            size.width, size.height,
            isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        )
        val config = fitSimpleWidgetConfig(
            context, baseConfig, size, reading.value, reading.unit, reading.measurement, reading.isAqi
        )
        val layout = if (reading.isAqi) {
            measureAqiLayout(context, config, reading.measurement, reading.value)
        } else {
            measureMeasurementLayout(context, config, reading.unit, reading.measurement, reading.value)
        }
        val scenario = "${reading.type} / ${context.resources.configuration} / $size"
        val valueTop = top(root, value)
        val valueMetrics = GlanceFontUtils.measureCustomFontMetrics(
            context, layout.valueFontSize, R.font.oswald_bold, reading.value,
            trimVerticalPadding = true, stableNumericBounds = true
        )
        val valueGlyphTop = bitmapY(root, value, valueMetrics.glyphTop.value * density)
        val valueBaseline = bitmapY(root, value, valueMetrics.baseline.value * density)
        val measurementGlyphTop = top(root, measurement) + measurement.baseline + textBounds(measurement).top
        val unitRowHeight = (layout.labelHeights.unit.value * density).roundToInt()
        val measurementRowHeight = (layout.labelHeights.measurement.value * density).roundToInt()
        val midpoint = valueTop + unitRowHeight
        val readingBottom = valueTop + (layout.textLayout.height.value * density).roundToInt()

        assertEquals("$scenario: label height budgets span the value", value.height, unitRowHeight + measurementRowHeight)
        assertTrue("$scenario: label height budgets differ by at most one pixel", unitRowHeight - measurementRowHeight in 0..1)
        assertLabelsFillHalfHeights(context, layout, reading.unit, reading.measurement)

        if (unit != null) {
            val unitMetrics = GlanceFontUtils.measureCustomFontMetrics(
                context, layout.labelFontSize, R.font.oswald_regular, reading.unit, trimVerticalPadding = true
            )
            assertEquals(
                "$scenario: unit glyphs align with the top of the number",
                valueGlyphTop,
                bitmapY(root, unit, unitMetrics.glyphTop.value * density),
                1f
            )
            assertEquals(
                "$scenario: the unit must not be scaled or clipped",
                (unitMetrics.height.value * density).roundToInt(),
                unit.height
            )
            assertTrue("$scenario: unit stays in its half", top(root, unit) + unit.height <= midpoint + 1f)
            assertTrue(
                "$scenario: unit and measurement glyphs must not overlap",
                bitmapY(root, unit, unitMetrics.glyphBottom.value * density) <= measurementGlyphTop + 1f
            )
            if (reading.isAqi) {
                val indicatorPixels = (layout.indicatorSize.value * density).roundToInt()
                val indicator = root.descendants.filterIsInstance<ImageView>().single {
                    it.contentDescription == null && it.width == indicatorPixels && it.height == indicatorPixels
                }
                assertTrue("$scenario: indicator stays in the upper half", top(root, indicator) >= valueTop)
                assertTrue(
                    "$scenario: indicator does not overlap the measurement",
                    top(root, indicator) + indicator.height <= midpoint + 1f
                )
            }
            assertEquals(
                "$scenario: unit bitmap must contain the full unit",
                (GlanceFontUtils.measureCustomFontWidth(
                    context, reading.unit, layout.labelFontSize, R.font.oswald_regular
                ).value * density).roundToInt(),
                (unit.drawable as BitmapDrawable).bitmap.width
            )
        }
        assertEquals(
            "$scenario: measurement and number share the bottom baseline",
            valueBaseline,
            top(root, measurement) + measurement.baseline,
            1f
        )
        val measurementMetrics = GlanceFontUtils.measureSystemFontMetrics(
            context, reading.measurement, layout.measurementNameFontSize, false
        )
        assertEquals(
            "$scenario: measurement descenders must not be clipped",
            (measurementMetrics.height.value * density).roundToInt(),
            measurement.height
        )
        assertTrue(
            "$scenario: the reading reserves room for measurement descenders",
            top(root, measurement) + measurement.height <= readingBottom + 1f
        )
        assertEquals(
            "$scenario: value bitmap must contain the full reading",
            (GlanceFontUtils.measureCustomFontWidth(
                context, reading.value, layout.valueFontSize, R.font.oswald_bold
            ).value * density).roundToInt(),
            (value.drawable as BitmapDrawable).bitmap.width
        )
        assertEquals("$scenario: measurement stays on one line", 1, measurement.lineCount)
        assertTrue(
            "$scenario: description must not be clipped",
            top(root, measurement) + measurement.height <= root.height
        )
        assertTrue(
            "$scenario: sensor name must precede the value",
            top(root, displayName) + displayName.height <= top(root, value)
        )
        assertTrue(
            "$scenario: timestamp must follow the measurement label",
            top(root, measurement) + measurement.height <= top(root, timestamp)
        )
        assertTrue(
            "$scenario: timestamp must follow the full value",
            top(root, value) + value.height <= top(root, timestamp)
        )
        assertTrue(
            "$scenario: timestamp must not be clipped",
            top(root, timestamp) + timestamp.height <= root.height
        )
    }

    private fun assertLabelsFillHalfHeights(
        context: Context,
        layout: MeasuredSimpleWidgetLayout,
        unit: String,
        measurement: String
    ) {
        val density = context.resources.displayMetrics.density
        val unitRowHeight = (layout.labelHeights.unit.value * density).roundToInt()
        val measurementRowHeight = (layout.labelHeights.measurement.value * density).roundToInt()
        if (unit.isNotBlank()) {
            val unitMetrics = GlanceFontUtils.measureCustomFontMetrics(
                context, layout.labelFontSize, R.font.oswald_regular, unit, trimVerticalPadding = true
            )
            val height = (unitMetrics.height.value * density).roundToInt()
            assertTrue("The unit must fit its half", height <= unitRowHeight)
            assertTrue("The unit must grow to fill its half", height >= unitRowHeight - 2)
        }
        val measurementMetrics = GlanceFontUtils.measureSystemFontMetrics(
            context, measurement, layout.measurementNameFontSize, false
        )
        val height = (measurementMetrics.height.value * density).roundToInt()
        assertTrue("The measurement name must fit its half", height <= measurementRowHeight)
        assertTrue("The measurement name must grow to fill its half", height >= measurementRowHeight - 2)
    }

    private fun applyWidget(context: Context, remoteViews: RemoteViews, size: DpSize): ViewGroup {
        val density = context.resources.displayMetrics.density
        return (remoteViews.apply(context, FrameLayout(context)) as ViewGroup).apply {
            measure(
                View.MeasureSpec.makeMeasureSpec(
                    (size.width.value * density).roundToInt(), View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(
                    (size.height.value * density).roundToInt(), View.MeasureSpec.EXACTLY
                )
            )
            layout(0, 0, measuredWidth, measuredHeight)
        }
    }

    private fun inkBounds(bitmap: Bitmap): Rect {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val firstPixel = pixels.indexOfFirst { Color.alpha(it) > 0 }
        val lastPixel = pixels.indexOfLast { Color.alpha(it) > 0 }
        assertTrue("The numeric bitmap must contain visible glyphs", firstPixel >= 0)
        return Rect(0, firstPixel / bitmap.width, bitmap.width, lastPixel / bitmap.width + 1)
    }

    private data class Reading(
        val value: String,
        val unit: String,
        val measurement: String,
        val type: WidgetType
    ) {
        val isAqi get() = type == WidgetType.AIR_QUALITY

        fun state() = preferencesOf(
            SimpleWidgetPrefKeys.displayName to "Living room",
            SimpleWidgetPrefKeys.sensorValue to value,
            SimpleWidgetPrefKeys.unit to unit,
            SimpleWidgetPrefKeys.measurementName to measurement,
            SimpleWidgetPrefKeys.measurementType to type.code.toString(),
            SimpleWidgetPrefKeys.updated to "12:34"
        )
    }

    private fun top(root: ViewGroup, view: View): Float =
        Rect(0, 0, view.width, view.height).also {
            root.offsetDescendantRectToMyCoords(view, it)
        }.top.toFloat()

    private fun bitmapY(root: ViewGroup, view: ImageView, bitmapY: Float): Float {
        val drawable = view.drawable as BitmapDrawable
        val point = floatArrayOf(0f, bitmapY * drawable.intrinsicHeight / drawable.bitmap.height)
        view.imageMatrix.mapPoints(point)
        return top(root, view) + view.paddingTop + point[1]
    }

    private fun textBounds(view: TextView): Rect {
        val paint = TextPaint(view.paint)
        val text = view.text
        if (text is Spanned) {
            text.getSpans(0, text.length, MetricAffectingSpan::class.java).forEach {
                it.updateMeasureState(paint)
            }
        }
        return Rect().also { paint.getTextBounds(text.toString(), 0, text.length, it) }
    }

}
