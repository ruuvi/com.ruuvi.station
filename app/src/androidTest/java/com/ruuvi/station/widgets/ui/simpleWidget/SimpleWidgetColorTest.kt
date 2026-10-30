package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.descendants
import androidx.core.view.drawToBitmap
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import androidx.test.InstrumentationRegistry
import com.ruuvi.station.R
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.WidgetRefreshButtonDefaults
import java.util.concurrent.FutureTask
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SimpleWidgetColorTest {
    @Test
    fun runtimeAndGeneratedPreviewColorsMatchInBothThemes() = runBlocking {
        val size = DpSize(220.dp, 140.dp)
        for (context in themedContexts()) {
            for (mode in SimpleWidgetRenderingMode.entries) {
                for ((value, unit, type) in listOf(
                    Triple("21.5", "\u00b0C", WidgetType.TEMPERATURE),
                    Triple("100", "/100", WidgetType.AIR_QUALITY)
                )) {
                    val widget = object : GlanceAppWidget() {
                        override val sizeMode = SizeMode.Exact

                        override suspend fun provideGlance(context: Context, id: GlanceId) {
                            provideContent {
                                SimpleWidgetContent(
                                    sensorId = null,
                                    displayName = "Sensor",
                                    sensorValue = value,
                                    unit = unit,
                                    measurementName = if (type == WidgetType.AIR_QUALITY) "Air quality" else "Temperature",
                                    updated = "12:34",
                                    measurementType = type,
                                    renderingMode = mode
                                )
                            }
                        }
                    }
                    val remoteViews = widget.compose(context = context, size = size)
                    onMain {
                        val root = remoteViews.apply(context, FrameLayout(context)) as ViewGroup
                        layout(root, context, size)
                        val title = root.descendants.filterIsInstance<TextView>()
                            .single { it.text.toString() == "Sensor" }
                        val number = root.descendants.filterIsInstance<ImageView>()
                            .single { it.contentDescription == value }
                        val unitImage = root.descendants.filterIsInstance<ImageView>()
                            .single { it.contentDescription == unit }
                        val scenario = "$mode / $type / ${context.resources.configuration.uiMode}"

                        val isNightMode = context.resources.configuration.uiMode and
                            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                        val expectedTitleColor = if (isNightMode) Color.WHITE else GlanceColors.resolvedValueColor(context)
                        assertEquals(expectedTitleColor, title.currentTextColor)
                        val rendered = root.drawToBitmap()
                        val background = rendered.getPixel(root.width / 2, 1)
                        assertArgbMatches(
                            "$scenario: number matches title",
                            dominantForeground(rendered, bounds(root, title), background),
                            dominantForeground(rendered, bounds(root, number), background)
                        )
                        if (mode == SimpleWidgetRenderingMode.GENERATED_PREVIEW) {
                            assertColorMatches(
                                "$scenario: unit retains its embedded color",
                                GlanceColors.resolvedValueColor(context),
                                unitImage
                            )
                        }
                        val iconSize = (
                            WidgetRefreshButtonDefaults.iconSize.value * context.resources.displayMetrics.density
                        ).roundToInt()
                        val refresh = root.descendants.filterIsInstance<ImageView>().single {
                            it.contentDescription == null && it.width == iconSize && it.height == iconSize
                        }
                        assertRefreshBackgroundMatches(root, refresh)
                    }
                }
            }
        }
    }

    @Test
    fun pickerAndLoadingLayoutsKeepTheirColorsConsistent() {
        for (context in themedContexts()) {
            onMain {
                val picker = LayoutInflater.from(context)
                    .inflate(R.layout.simple_widget_picker_preview, null) as ViewGroup
                layout(picker, context, DpSize(180.dp, 110.dp))
                val title = picker.descendants.filterIsInstance<TextView>()
                    .single { it.text.toString() == context.getString(R.string.widget_preview_sensor_name) }
                val value = picker.findViewById<TextView>(R.id.simplePreviewValue)

                assertEquals(title.currentTextColor, value.currentTextColor)
                assertRefreshBackgroundMatches(picker, picker.descendants.filterIsInstance<ImageView>().single())

                val loading = LayoutInflater.from(context)
                    .inflate(R.layout.widget_simple_preview, null) as ViewGroup
                layout(loading, context, DpSize(180.dp, 110.dp))
                assertRefreshBackgroundMatches(loading, loading.descendants.filterIsInstance<ImageView>().single())
            }
        }
    }

    @Test
    fun legacyPickerNumberMatchesItsTitleInBothThemes() {
        for (context in themedContexts()) {
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.simple_widget_preview_legacy)
            val background = bitmap.getPixel(256, 8)
            val title = dominantForeground(bitmap, Rect(30, 20, 330, 66), background)
            val number = dominantForeground(bitmap, Rect(30, 80, 172, 142), background)

            assertEquals(title, number)
        }
    }

    private fun themedContexts(): List<Context> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return listOf(Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES).map { nightMode ->
            context.createConfigurationContext(Configuration(context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightMode
            })
        }
    }

    private fun onMain(assertion: () -> Unit) {
        val task = FutureTask(assertion)
        InstrumentationRegistry.getInstrumentation().runOnMainSync(task)
        task.get()
    }

    private fun layout(root: ViewGroup, context: Context, size: DpSize) {
        val density = context.resources.displayMetrics.density
        val width = (size.width.value * density).roundToInt()
        val height = (size.height.value * density).roundToInt()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, width, height)
    }

    private fun assertColorMatches(message: String, expected: Int, image: ImageView) {
        val bitmap = image.drawToBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val actual = pixels.maxBy { Color.alpha(it) }
        assertTrue("$message: visible glyphs", Color.alpha(actual) > 0)
        assertArgbMatches(message, expected, actual)
    }

    private fun assertArgbMatches(message: String, expected: Int, actual: Int) {
        val detail = "$message (expected=${expected.toUInt().toString(16)}, actual=${actual.toUInt().toString(16)})"
        assertEquals("$detail: alpha", Color.alpha(expected), Color.alpha(actual))
        // Premultiplied bitmap colors can round a channel by one when read back.
        assertTrue("$detail: red", abs(Color.red(expected) - Color.red(actual)) <= 1)
        assertTrue("$detail: green", abs(Color.green(expected) - Color.green(actual)) <= 1)
        assertTrue("$detail: blue", abs(Color.blue(expected) - Color.blue(actual)) <= 1)
    }

    private fun assertRefreshBackgroundMatches(root: ViewGroup, refresh: ImageView) {
        val bounds = bounds(root, refresh)
        val bitmap = root.drawToBitmap()
        val background = bitmap.getPixel(root.width / 2, 1)

        assertEquals(
            "The refresh icon's transparent center must show the widget background",
            background,
            bitmap.getPixel(bounds.centerX(), bounds.centerY())
        )
    }

    private fun bounds(root: ViewGroup, view: View): Rect =
        Rect(0, 0, view.width, view.height).also {
            root.offsetDescendantRectToMyCoords(view, it)
        }

    private fun dominantForeground(bitmap: Bitmap, bounds: Rect, background: Int): Int {
        val pixels = IntArray(bounds.width() * bounds.height())
        bitmap.getPixels(pixels, 0, bounds.width(), bounds.left, bounds.top, bounds.width(), bounds.height())
        return pixels.filter { it != background && Color.alpha(it) > 0 }
            .groupingBy { it }
            .eachCount()
            .maxBy { it.value }
            .key
    }
}
