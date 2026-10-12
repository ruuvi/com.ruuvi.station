package com.ruuvi.station.history

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.ruuvi.station.graph.addDataToChart
import com.ruuvi.station.graph.applyChartStyle
import org.junit.Assert.*
import com.ruuvi.station.graph.CustomXAxisRenderer
import com.ruuvi.station.graph.synchronizeChartGestures
import com.github.mikephil.charting.components.YAxis
import java.time.Instant
import java.util.Calendar
import java.util.TimeZone
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryChartRenderingTest {
    @Test fun `400 points render across gaps while zooming with datapoints enabled`() {
        renderChangingViewports(400, true)
    }

    @Test fun `1000 points render across gaps while zooming with datapoints enabled`() {
        renderChangingViewports(1000, true)
    }

    @Test fun `isolated measurements render while zooming with datapoints disabled`() {
        renderChangingViewports(400, false, segmentSize = 1)
    }

    @Test fun `three year axis has bounded year labels and switches to times when zoomed`() {
        val chart = chart(1080)
        val duration = 1095 * 86_400_000L
        val origin = Instant.parse("2023-09-16T00:00:00Z").toEpochMilli()
        addDataToChart(chart.context, mutableListOf(Entry(0f, 1f), Entry(duration.toFloat(), 2f)),
            chart, "", false, null, origin, origin + duration, origin)
        val renderer = CustomXAxisRenderer(origin, chart.viewPortHandler, chart.xAxis,
            chart.getTransformer(YAxis.AxisDependency.LEFT))
        renderer.computeAxis(0f, duration.toFloat(), false)
        assertTrue(chart.xAxis.mEntryCount <= chart.xAxis.labelCount + 6)
        val labels = chart.xAxis.mEntries.map { chart.xAxis.valueFormatter.getFormattedValue(it, chart.xAxis) }
        assertTrue(labels.any { it.contains("2024") })
        assertTrue(labels.any { it.contains("2025") })
        chart.viewPortHandler.refresh(Matrix().apply { setScale(1095 * 24f, 1f) }, chart, false)
        val label = chart.xAxis.valueFormatter.getFormattedValue(1_800_000.0, chart.xAxis)
        assertFalse(label.contains("2023"))
        assertTrue(label.contains(":"))
    }

    @Test fun `day ticks remain local midnight across DST and fractional timezone offsets`() {
        val previous = TimeZone.getDefault()
        try {
            for (zone in listOf("Europe/Helsinki", "Asia/Kathmandu")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                for (start in listOf("2026-03-27T00:00:00Z", "2026-10-23T00:00:00Z")) {
                    val origin = Instant.parse(start).toEpochMilli()
                    val chart = chart(1080)
                    chart.xAxis.setLabelCount(4, false)
                    val renderer = CustomXAxisRenderer(origin, chart.viewPortHandler, chart.xAxis,
                        chart.getTransformer(YAxis.AxisDependency.LEFT))
                    renderer.computeAxis(0f, (3 * 86_400_000L).toFloat(), false)
                    assertTrue(chart.xAxis.mEntries.isNotEmpty())
                    for (entry in chart.xAxis.mEntries) {
                        val tick = Calendar.getInstance().apply { timeInMillis = origin + entry.toLong() }
                        assertEquals(zone, 0, tick.get(Calendar.HOUR_OF_DAY))
                        assertEquals(zone, 0, tick.get(Calendar.MINUTE))
                    }
                }
            }
        } finally { TimeZone.setDefault(previous) }
    }

    @Test fun `synchronized charts with different widths keep the same horizontal viewport`() {
        val source = chart(1080)
        val follower = chart(720)
        val duration = 1095 * 86_400_000L
        for (chart in listOf(source, follower)) {
            addDataToChart(chart.context, mutableListOf(Entry(0f, 1f), Entry(duration.toFloat(), 2f)),
                chart, "", false, null, 0, duration, 0)
        }
        val callbacks = mutableListOf<LineChart>()
        synchronizeChartGestures(setOf(source, follower)) { chart, _ -> callbacks.add(chart) }
        source.viewPortHandler.refresh(Matrix().apply { setScale(3f, 1f); postTranslate(-100f, 0f) }, source, false)
        source.onChartGestureListener.onChartScale(null, 3f, 1f)
        val a = FloatArray(9).also { source.viewPortHandler.matrixTouch.getValues(it) }
        val b = FloatArray(9).also { follower.viewPortHandler.matrixTouch.getValues(it) }
        assertEquals(a[Matrix.MSCALE_X], b[Matrix.MSCALE_X], 0.0001f)
        assertEquals(a[Matrix.MTRANS_X] / source.viewPortHandler.contentWidth(),
            b[Matrix.MTRANS_X] / follower.viewPortHandler.contentWidth(), 0.0001f)
        assertEquals(listOf(source), callbacks)
    }

    private fun chart(width: Int): LineChart {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return LineChart(context).apply {
            applyChartStyle(context, this)
            measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
            layout(0, 0, width, 600)
        }
    }

    private fun renderChangingViewports(count: Int, drawDots: Boolean, segmentSize: Int = 3) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bitmap = Bitmap.createBitmap(1080, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val duration = 1095 * 86_400_000L
        for (limits in listOf(null, 5.0 to 20.0)) {
            val chart = LineChart(context)
            applyChartStyle(context, chart)
            chart.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
            chart.layout(0, 0, 1080, 600)
            for (scale in listOf(1f, 5f, 100f, 5000f, 100f, 1f)) {
                for (size in listOf(count, 2, 1, 0, count)) {
                    val entries = (0 until size).map { i ->
                        val time = i * duration / size
                        Entry(time.toFloat(), (i % 31).toFloat(), HistoryPoint(time, (i % 31).toDouble(), i / segmentSize))
                    }.toMutableList()
                    addDataToChart(context, entries, chart, "", drawDots, limits, 0, duration, 0)
                    val matrix = Matrix().apply { setScale(scale, 1f); postTranslate(-1080f * (scale - 1) / 2, 0f) }
                    chart.viewPortHandler.refresh(matrix, chart, false)
                    chart.draw(canvas)
                }
            }
        }
        bitmap.recycle()
    }
}
