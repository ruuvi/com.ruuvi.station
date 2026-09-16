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

    private fun renderChangingViewports(count: Int, drawDots: Boolean, segmentSize: Int = 3) {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val bitmap = Bitmap.createBitmap(1080, 600, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val duration = 100 * 86_400_000L
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
