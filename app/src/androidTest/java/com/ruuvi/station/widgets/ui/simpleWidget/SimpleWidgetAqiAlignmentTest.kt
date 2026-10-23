package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import android.content.res.Configuration
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
                            val remoteViews = SimpleWidgetGlanceWidget.compose(
                                context = context,
                                size = size,
                                state = reading.state()
                            )
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
    fun labelFontCanGrowOrShrinkWithoutLeavingASpareGap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val assertion = FutureTask {
            val context = instrumentation.targetContext
            val base = SimpleWidgetLayoutConfig.fromSize(188.dp, 96.dp)
            for (config in listOf(
                base.copy(valueFontSize = 60.sp, secondaryFontSize = 8.sp),
                base.copy(valueFontSize = 12.sp, secondaryFontSize = 24.sp)
            )) {
                val layout = measureAqiLayout(context, config, "Air quality")
                val originalLabelSize = config.secondaryFontSize.toWidgetSp(context).value
                if (config.valueFontSize > config.secondaryFontSize) {
                    assertTrue(layout.labelFontSize.value > originalLabelSize)
                } else {
                    assertTrue(layout.labelFontSize.value < originalLabelSize)
                }
                assertCompactCapHeight(context, config, layout, "/100", "Air quality")
            }
        }
        instrumentation.runOnMainSync(assertion)
        assertion.get()
    }

    @Test
    fun valueFontCanShrinkAsWellAsGrowToMatchTheLabelHeight() {
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
    fun unitlessReadingKeepsItsMeasurementOnTheValueBaseline() = runBlocking<Unit> {
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

    private fun assertReadingLayout(
        context: Context,
        remoteViews: RemoteViews,
        size: DpSize,
        reading: Reading
    ) {
        val density = context.resources.displayMetrics.density
        val root = remoteViews.apply(context, FrameLayout(context)) as ViewGroup
        root.measure(
            View.MeasureSpec.makeMeasureSpec(
                (size.width.value * density).roundToInt(), View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                (size.height.value * density).roundToInt(), View.MeasureSpec.EXACTLY
            )
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
        val value = root.descendants.filterIsInstance<ImageView>()
            .single { it.contentDescription == reading.value }
        val unit = if (reading.unit.isBlank()) null else {
            root.descendants.filterIsInstance<TextView>()
                .single { it.text.toString() == reading.unit }
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
            measureAqiLayout(context, config, reading.measurement)
        } else {
            measureMeasurementLayout(context, config, reading.unit, reading.measurement)
        }
        val metrics = GlanceFontUtils.measureCustomFontMetrics(
            context, layout.valueFontSize, R.font.oswald_bold
        )
        val scenario = "${reading.type} / ${context.resources.configuration} / $size"

        if (unit != null) {
            assertEquals(
                "$scenario: unit cap line",
                bitmapY(root, value, metrics.glyphTop.value * density), glyphTop(root, unit), 1.5f
            )
            var upperContentBottom = glyphBottom(root, unit)
            if (reading.isAqi) {
                val indicatorPixels = (layout.indicatorSize.value * density).roundToInt()
                val indicator = root.descendants.filterIsInstance<ImageView>().single {
                    it.contentDescription == null && it.width == indicatorPixels && it.height == indicatorPixels
                }
                upperContentBottom = maxOf(upperContentBottom, top(root, indicator) + indicator.height)
            }
            val gap = glyphTop(root, measurement) - upperContentBottom
            assertTrue("$scenario: labels must not overlap (gap=$gap)", gap >= 0f)
            assertTrue(
                "$scenario: label gap must stay compact (gap=$gap)",
                gap <= config.inlineSpacing.value * density + 2f
            )
            assertCompactCapHeight(context, config, layout, reading.unit, reading.measurement)
            assertEquals("$scenario: unit must not be ellipsized", 0, unit.layout.getEllipsisCount(0))
        }
        assertEquals(
            "$scenario: description baseline",
            bitmapY(root, value, metrics.baseline.value * density),
            top(root, measurement) + measurement.baseline, 1.5f
        )
        assertEquals(
            "$scenario: value bitmap must contain the full reading",
            (GlanceFontUtils.measureCustomFontWidth(
                context, reading.value, layout.valueFontSize, R.font.oswald_bold
            ).value * density).roundToInt(),
            (value.drawable as BitmapDrawable).bitmap.width
        )
        assertEquals("$scenario: measurement must not be ellipsized", 0, measurement.layout.getEllipsisCount(0))
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
            "$scenario: timestamp must not be clipped",
            top(root, timestamp) + timestamp.height <= root.height
        )
    }

    private fun assertCompactCapHeight(
        context: Context,
        config: SimpleWidgetLayoutConfig,
        layout: MeasuredSimpleWidgetLayout,
        unit: String,
        measurement: String
    ) {
        val valueMetrics = GlanceFontUtils.measureCustomFontMetrics(
            context, layout.valueFontSize, R.font.oswald_bold
        )
        val minimumHeight = calculateAqiMinimumValueHeight(
            GlanceFontUtils.measureSystemFontMetrics(context, unit, layout.labelFontSize, true),
            GlanceFontUtils.measureSystemFontMetrics(context, measurement, layout.labelFontSize, false),
            layout.indicatorSize,
            config.inlineSpacing
        )
        val density = context.resources.displayMetrics.density
        val minimumPixels = ceil(minimumHeight.value * density)
        val capPixels = (valueMetrics.baseline - valueMetrics.glyphTop).value * density
        assertTrue(
            "Value cap height must fit the label stack ($capPixels / $minimumPixels, $unit, ${layout.valueFontSize})",
            capPixels + 0.01f >= minimumPixels
        )
        // Oswald hinting can jump by two pixels, leaving one cap height unavailable.
        assertTrue(
            "Value cap height must match the smallest fitting label stack ($capPixels / $minimumPixels)",
            capPixels <= minimumPixels + 1.01f
        )
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

    private fun glyphTop(root: ViewGroup, view: TextView): Float =
        top(root, view) + view.baseline + textBounds(view).top

    private fun glyphBottom(root: ViewGroup, view: TextView): Float =
        top(root, view) + view.baseline + textBounds(view).bottom
}
