package com.ruuvi.station.util

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.os.Handler
import android.support.v4.content.res.ResourcesCompat
import android.view.MotionEvent
import android.view.View
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.ruuvi.station.R
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.model.GraphEntry
import com.ruuvi.station.model.TagSensorReading
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GraphView(val context: Context) {
    private var from: Long = 0
    private var to: Long = 0
    val handler = Handler()
    val temperatureUnit: String = RuuviTagRepository.getTemperatureUnit(context)
    private val TEMP = "Temperature"
    private val HUMIDITY = "Humidity"
    private val PRESSURE = "Pressure"

    fun drawChart(tagId: String, view: View) {
        val readings = TagSensorReading.getForTag(tagId)

        val tempChart: LineChart = view.findViewById(R.id.tempChart)
        val humidChart: LineChart = view.findViewById(R.id.humidChart)
        val pressureChart: LineChart = view.findViewById(R.id.pressureChart)

        val tempData: MutableList<Entry> = ArrayList()
        val humidData: MutableList<Entry> = ArrayList()
        val pressureData: MutableList<Entry> = ArrayList()

        val cal = Calendar.getInstance()
        to = cal.time.time;
        cal.add(Calendar.HOUR, -24)
        from = cal.time.time

        if (readings.size > 0) {
            if (from < readings[0].createdAt.time)
                from = readings[0].createdAt.time

            val viewFrom = tempChart.lowestVisibleX
            val viewTo = if (tempChart.viewPortHandler.scaleX == 1.0f) (to - from).toFloat() else tempChart.highestVisibleX

            val entries = readings.map {
                GraphEntry(
                        timestamp = (it.createdAt.time - from).toFloat(),
                        temperature = when {
                            temperatureUnit.equals("K") -> {
                                Utils.celsiusToKelvin(it.temperature).toFloat()
                            }
                            temperatureUnit.equals("F") -> {
                                Utils.celciusToFahrenheit(it.temperature).toFloat()
                            }
                            else -> {
                                it.temperature.toFloat()
                            }
                        },
                        humidity = it.humidity.toFloat(),
                        pressure = it.pressure.toFloat() / 100.0f
                )
            }
            val viewPortCount = entries.count { it.timestamp >= viewFrom && it.timestamp <= viewTo }

            if (viewPortCount > 100) {
                val step = (viewTo - viewFrom) / 100.0f
                var current = 0f
                entries.forEach {
                    if (it.timestamp >= current) {
                        tempData.add(Entry(it.timestamp, it.temperature))
                        humidData.add(Entry(it.timestamp, it.humidity))
                        pressureData.add(Entry(it.timestamp, it.pressure))
                        current += step
                    }
                }
            } else {
                entries.forEach {
                    tempData.add(Entry(it.timestamp, it.temperature))
                    humidData.add(Entry(it.timestamp, it.humidity))
                    pressureData.add(Entry(it.timestamp, it.pressure))
                }
            }
            Timber.d("readings = ${readings.size} viewCount = ${viewPortCount} filtered = ${tempData.size}")
        } else {
            val timestamp = to.toFloat()
            tempData.add(Entry(timestamp, Utils.celciusToFahrenheit(0.0).toFloat()))
            humidData.add(Entry(timestamp, 0f))
            pressureData.add(Entry(timestamp, 0f))
        }

        addDataToChart(tempData, tempChart, TEMP)
        addDataToChart(humidData, humidChart, HUMIDITY)
        addDataToChart(pressureData, pressureChart, PRESSURE)

        tempChart.setViewPortOffsets(
                pressureChart.viewPortHandler.offsetLeft(),
                pressureChart.viewPortHandler.offsetTop(),
                pressureChart.viewPortHandler.offsetRight(),
                pressureChart.viewPortHandler.offsetBottom()
        )
        humidChart.setViewPortOffsets(
                pressureChart.viewPortHandler.offsetLeft(),
                pressureChart.viewPortHandler.offsetTop(),
                pressureChart.viewPortHandler.offsetRight(),
                pressureChart.viewPortHandler.offsetBottom()
        )

        synchronizeChartGestures(setOf(tempChart, humidChart, pressureChart))
    }

    fun addDataToChart(data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.highLightColor = context.resources.getColor(R.color.main)
        set.circleRadius = 2f
        set.setDrawCircles(isZoomed(chart))
        chart.setPinchZoom(true)
        chart.xAxis.axisMaximum = (to - from).toFloat()
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
        chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
        chart.description.text = label
        chart.description.textColor = Color.WHITE
        chart.description.textSize = context.resources.getDimension(R.dimen.graph_description_size)
        chart.dragDecelerationFrictionCoef = 0.8f
        chart.setNoDataTextColor(Color.WHITE)
        val scale = if (data.size < 100) 50f else data.size.toFloat()
        chart.viewPortHandler.setMaximumScaleX(scale)
        chart.viewPortHandler.setMaximumScaleY(30f)
        try {
            chart.description.typeface = ResourcesCompat.getFont(context, R.font.montserrat)
        } catch (e: Exception) { /* ¯\_(ツ)_/¯ */
        }
        chart.legend.isEnabled = false
        chart.data = LineData(set)
        chart.data.isHighlightEnabled = false

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return mFormat.format(Date(value.toLong() + from))
            }
        }
        chart.axisLeft.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return "%.1f".format(value)
            }
        }
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    /**
     * Binds [charts] together so that pinch and pan gestures
     * in one apply to all of them.
     */
    private fun synchronizeChartGestures(charts: Set<LineChart>) {
        var srcChart: LineChart? = null

        val sync = object : Runnable {
            override fun run() {
                srcChart?.let { chart ->
                    val srcVals = FloatArray(9)
                    chart.viewPortHandler.matrixTouch.getValues(srcVals)

                    charts.minus(chart).forEach { dstChart: LineChart ->
                        val dstMatrix = dstChart.viewPortHandler.matrixTouch
                        val dstVals = FloatArray(9)
                        dstMatrix.getValues(dstVals)
                        dstVals[Matrix.MSCALE_X] = srcVals[Matrix.MSCALE_X]
                        dstVals[Matrix.MTRANS_X] = srcVals[Matrix.MTRANS_X]
                        dstVals[Matrix.MSKEW_X] = srcVals[Matrix.MSKEW_X]
                        dstMatrix.setValues(dstVals)
                        dstChart.viewPortHandler.refresh(dstMatrix, dstChart, true)
                    }
                }
            }
        }

        charts.forEach { chart: LineChart ->
            chart.onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureEnd(
                        me: MotionEvent?,
                        lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                    srcChart = chart
                    handler.postDelayed(sync, 500L)
                    charts.forEach {
                        it.setTouchEnabled(true)
                    }
                }
                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartSingleTapped(me: MotionEvent?) {}
                override fun onChartGestureStart(
                        me: MotionEvent?,
                        lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                    handler.removeCallbacks(sync)
                    charts.forEach {
                        if (it != chart)
                            it.setTouchEnabled(false)
                    }
                }
                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                    charts.forEach {
                        val set = it.data.maxEntryCountSet as LineDataSet
                        set.setDrawCircles(isZoomed(chart))
                    }
                    srcChart = chart
                    handler.post(sync)
                }
                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {}
            }
        }
    }

    private fun isZoomed(chart: LineChart): Boolean {
        val zoomStartValue = 1.5f
        val zoomEndValue = 15.0f
        return chart.scaleX >= zoomStartValue && chart.scaleX < zoomEndValue || chart.scaleY >= zoomStartValue && chart.scaleY < zoomEndValue
    }
}