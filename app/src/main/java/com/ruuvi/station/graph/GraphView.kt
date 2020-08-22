package com.ruuvi.station.graph

import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import androidx.core.content.res.ResourcesCompat
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.util.Utils
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GraphView {
    private var from: Long = 0
    private var to: Long = 0
    private val TEMP = "Temperature"
    private val HUMIDITY = "Humidity"
    private val PRESSURE = "Pressure"
    private var storedReadings: MutableList<TagSensorReading>? = null

    fun drawChart(inputReadings: List<TagSensorReading>, view: View, context: Context, repository: TagRepository) {
        Timber.d("drawChart pointsCount = ${inputReadings.size}")
        val tempChart: LineChart = view.findViewById(R.id.tempChart)
        tempChart.isVisible = true
        val humidChart: LineChart = view.findViewById(R.id.humidChart)
        humidChart.isVisible = true
        val pressureChart: LineChart = view.findViewById(R.id.pressureChart)
        pressureChart.isVisible = true
        val temperatureUnit: String = repository.getTemperatureUnit()

        if (storedReadings.isNullOrEmpty() || tempChart.highestVisibleX >= tempChart.data?.xMax ?: Float.MIN_VALUE) {
            val calendar = Calendar.getInstance()
            to = calendar.time.time
            calendar.add(Calendar.HOUR, -24)
            from = calendar.time.time
            storedReadings = inputReadings.toMutableList()
        }

        tempChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.2f")
        humidChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.2f")
        pressureChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.1f")

        val tempData: MutableList<Entry> = ArrayList()
        val humidData: MutableList<Entry> = ArrayList()
        val pressureData: MutableList<Entry> = ArrayList()

        storedReadings?.let { tagReadings ->
            if (tagReadings.size > 0) {
                from = tagReadings[0].createdAt.time

                val entries = tagReadings.map {
                    GraphEntry(
                        timestamp = (it.createdAt.time - from).toFloat(),
                        temperature = when (temperatureUnit) {
                            "K" -> {
                                Utils.celsiusToKelvin(it.temperature).toFloat()
                            }
                            "F" -> {
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

                entries.forEach {
                    tempData.add(Entry(it.timestamp, it.temperature))
                    humidData.add(Entry(it.timestamp, it.humidity))
                    pressureData.add(Entry(it.timestamp, it.pressure))
                }
            } else {
                val timestamp = to.toFloat()
                tempData.add(Entry(timestamp, 0f))
                humidData.add(Entry(timestamp, 0f))
                pressureData.add(Entry(timestamp, 0f))
            }
            addDataToChart(context, tempData, tempChart, TEMP)
            addDataToChart(context, humidData, humidChart, HUMIDITY)
            addDataToChart(context, pressureData, pressureChart, PRESSURE)

            normalizeOffsets(tempChart, humidChart, pressureChart)
            synchronizeChartGestures(setOf(tempChart, humidChart, pressureChart))
        }
    }

    private fun addDataToChart(context: Context, data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawCircles(false)
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.highLightColor = ContextCompat.getColor(context, R.color.main)
        set.circleRadius = 2f
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
        chart.viewPortHandler.setMaximumScaleX(20000f)
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
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    /**
     * Binds [charts] together so that pinch and pan gestures
     * in one apply to all of them.
     */
    private fun synchronizeChartGestures(charts: Set<LineChart>) {
        fun synchronizeCharts(sourceChart: LineChart) {
            val sourceMatrixValues = FloatArray(9)
            sourceChart.viewPortHandler.matrixTouch.getValues(sourceMatrixValues)

            charts.forEach { targetChart: LineChart ->
                if (targetChart != sourceChart) {
                    val targetMatrix = targetChart.viewPortHandler.matrixTouch
                    val targetMatrixValues = FloatArray(9)
                    targetMatrix.getValues(targetMatrixValues)
                    targetMatrixValues[Matrix.MSCALE_X] = sourceMatrixValues[Matrix.MSCALE_X]
                    targetMatrixValues[Matrix.MTRANS_X] = sourceMatrixValues[Matrix.MTRANS_X]
                    targetMatrixValues[Matrix.MSKEW_X] = sourceMatrixValues[Matrix.MSKEW_X]
                    targetMatrix.setValues(targetMatrixValues)
                    targetChart.viewPortHandler.refresh(targetMatrix, targetChart, true)
                }
            }
        }

        charts.forEach { chart: LineChart ->
            chart.onChartGestureListener = object : OnChartGestureListener {
                override fun onChartGestureEnd(
                    me: MotionEvent?,
                    lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                    charts.forEach {
                        it.setTouchEnabled(true)
                    }
                    synchronizeCharts(chart)
                }

                override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
                override fun onChartSingleTapped(me: MotionEvent?) {}
                override fun onChartGestureStart(
                    me: MotionEvent?,
                    lastPerformedGesture: ChartTouchListener.ChartGesture?
                ) {
                    charts.minus(chart).forEach {
                        it.setTouchEnabled(false)
                    }
                }

                override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                    synchronizeCharts(chart)
                }

                override fun onChartLongPressed(me: MotionEvent?) {}
                override fun onChartDoubleTapped(me: MotionEvent?) {}
                override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                    synchronizeCharts(chart)
                }
            }
        }
    }

    private fun normalizeOffsets(tempChart: LineChart, humidChart: LineChart, pressureChart: LineChart) {
        if (pressureChart.viewPortHandler.offsetLeft() > tempChart.viewPortHandler.offsetLeft()) {
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
        } else {
            pressureChart.setViewPortOffsets(
                tempChart.viewPortHandler.offsetLeft(),
                tempChart.viewPortHandler.offsetTop(),
                tempChart.viewPortHandler.offsetRight(),
                tempChart.viewPortHandler.offsetBottom()
            )
            humidChart.setViewPortOffsets(
                tempChart.viewPortHandler.offsetLeft(),
                tempChart.viewPortHandler.offsetTop(),
                tempChart.viewPortHandler.offsetRight(),
                tempChart.viewPortHandler.offsetBottom()
            )
        }
    }

    class AxisLeftValueFormatter(private val formatPattern: String) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return formatPattern.format(value)
        }
    }
}