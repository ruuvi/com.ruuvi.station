package com.ruuvi.station.graph

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.GapEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.renderer.LineChartRenderer
import com.ruuvi.station.history.HistorySample
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HistoryRenderingTest {
    private val fixtures = listOf(
        listOf(1000f to 2f),
        listOf(0f to 2f, 1000f to 4f, 2000f to 1f),
        listOf(0f to 2f, 1000f to 4f, 7_200_000f to 1f),
        listOf(0f to 2f, 7_200_000f to 4f, 14_400_000f to 1f),
        listOf(0f to 2f, 7_200_000f to 4f, 7_201_000f to 1f)
    )
    private fun entries(values: List<Pair<Float, Float>>): List<Entry> {
        var segment = 0
        return values.mapIndexed { index, (x,y) ->
            if (index > 0 && x - values[index - 1].first > 3_600_000) segment++
            Entry(x, y, HistorySample(x.toLong() / 1000, y.toDouble(), segment))
        }
    }
    @Test fun `raw gap flags match the shipped renderer for unsampled readings`() {
        val chart = LineChart(RuntimeEnvironment.getApplication())
        val legacy = LineChartRenderer(chart, chart.animator, chart.viewPortHandler)
        val history = HistoryLineChartRenderer(chart)
        for (fixture in fixtures.filter { it.size > 1 }) for (limits in listOf(false, true)) {
            val set = LineDataSet(entries(fixture), "")
            val old = legacy.buildLineDataSetWithIntersections(set, if (limits) 3f else null, if (limits) 1.5f else null, 3_600_000f)
            val new = history.buildLineDataSetWithIntersections(set, if (limits) 3f else null, if (limits) 1.5f else null, 3_600_000f)
            assertEquals(old.entryCount, new.entryCount)
            for (i in 0 until old.entryCount) {
                val a = old.getEntryForIndex(i) as GapEntry; val b = new.getEntryForIndex(i) as GapEntry
                assertEquals("gap fixture=$fixture i=$i", a.isGap, b.isGap)
                assertEquals("single fixture=$fixture i=$i", a.isSinglePoint, b.isSinglePoint)
            }
        }
    }
    @Test fun `line fill dots alerts and isolated stems match pixel for pixel`() {
        for (fixture in fixtures.filter { it.size > 1 }) for (dots in listOf(false, true)) for (limits in listOf(false, true)) {
            fun render(history: Boolean): Bitmap {
                val chart = LineChart(RuntimeEnvironment.getApplication())
                chart.setViewPortOffsets(20f, 20f, 20f, 20f)
                chart.axisLeft.axisMinimum = 0f; chart.axisLeft.axisMaximum = 5f
                chart.axisRight.isEnabled = false; chart.axisLeft.isEnabled = false; chart.xAxis.isEnabled = false
                chart.legend.isEnabled = false; chart.description.isEnabled = false
                chart.xAxis.axisMinimum = -10000f; chart.xAxis.axisMaximum = maxOf(10000f, fixture.last().first + 10000)
                if (limits) { chart.axisLeft.addLimitLine(LimitLine(3f)); chart.axisLeft.addLimitLine(LimitLine(1.5f)) }
                val set = LineDataSet(entries(fixture), "")
                set.maximumGapBetweenPoints = 3_600_000f; set.lineWidth = 1f; set.circleRadius = 1.5f
                set.setDrawFilled(true); set.setDrawCircles(dots); set.setDrawCircleHole(false); set.setDrawValues(false)
                chart.data = LineData(set)
                if (history) chart.renderer = HistoryLineChartRenderer(chart)
                chart.measure(View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY))
                chart.layout(0,0,600,300); chart.notifyDataSetChanged()
                return Bitmap.createBitmap(600,300,Bitmap.Config.ARGB_8888).also { chart.draw(Canvas(it)) }
            }
            val expected = render(false); val actual = render(true)
            assertTrue("fixture=$fixture dots=$dots limits=$limits", expected.sameAs(actual))
            expected.recycle(); actual.recycle()
        }
    }
    @Test fun `a single reading uses the existing small isolated point stem style`() {
        val chart = LineChart(RuntimeEnvironment.getApplication())
        val result = HistoryLineChartRenderer(chart).buildLineDataSetWithIntersections(LineDataSet(entries(fixtures.first()), ""), null, null, 3_600_000f)
        assertTrue((result.getEntryForIndex(0) as GapEntry).isSinglePoint)
    }
    @Test fun `sampling cannot invent gaps in continuous readings`() {
        val chart = LineChart(RuntimeEnvironment.getApplication())
        val set = LineDataSet(listOf(Entry(0f,1f, HistorySample(0,1.0,0)), Entry(20_000_000f,2f,HistorySample(20000,2.0,0))), "")
        val result = HistoryLineChartRenderer(chart).buildLineDataSetWithIntersections(set,null,null,3_600_000f)
        assertFalse((result.getEntryForIndex(1) as GapEntry).isGap)
        assertFalse((result.getEntryForIndex(0) as GapEntry).isSinglePoint)
    }
    @Test fun `long axis remains bounded and dates round trip across DST`() {
        val chart = LineChart(RuntimeEnvironment.getApplication())
        chart.viewPortHandler.setChartDimens(600f,300f)
        chart.xAxis.setLabelCount(5, false)
        val renderer = HistoryXAxisRenderer(0, chart.viewPortHandler, chart.xAxis, chart.getTransformer(com.github.mikephil.charting.components.YAxis.AxisDependency.LEFT))
        renderer.computeAxis(0f, 1096 * 86400_000f, false)
        assertTrue(chart.xAxis.mEntryCount <= 12)
        val zone = TimeZone.getTimeZone("Europe/Oslo")
        val day = 1743292800000L // March 30 2025, the DST transition
        val start = calendarLocalDay(day, zone = zone); val end = calendarLocalDay(day, next = true, zone = zone)
        assertEquals(day, calendarUtcDay(start, zone)); assertEquals(23 * 3600000L, end - start)
        assertEquals(1096 * 24, historyDurationHours(1096, true).last())
        assertEquals(0, historyDurationHours(0, true).single())
        assertEquals(1096 + 6, historyDurationHours(1096, false).size)
        assertTrue(historyDurationHours(30,true).all { it <= 30 * 24 })
    }
}
