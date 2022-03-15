package com.ruuvi.station.graph

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Color
import android.graphics.Matrix
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.GraphEntry
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.PressureUnit
import timber.log.Timber
import java.text.DateFormat
import java.text.DateFormat.getTimeInstance
import java.util.*

class GraphView (
    private val unitsConverter: UnitsConverter,
    private val preferencesRepository: PreferencesRepository,
    private val context: Context
){
    private var from: Long = 0
    private var to: Long = 0
    private var storedReadings: MutableList<TagSensorReading>? = null
    private var graphSetupCompleted = false
    private var offsetsNormalized = false
    private var visibilitySet = false

    private lateinit var tempChart: LineChart
    private lateinit var humidChart: LineChart
    private lateinit var pressureChart: LineChart

    fun drawChart(
            inputReadings: List<TagSensorReading>,
            view: View
    ) {
        Timber.d("drawChart pointsCount = ${inputReadings.size}")
        setupCharts(view)

        val firstReading = inputReadings.firstOrNull()
        if (firstReading != null) {
            setupVisibility(
                view,
                true,
                firstReading.humidity != null,
                firstReading.pressure != null
            )
        }

        if (storedReadings.isNullOrEmpty() || tempChart.highestVisibleX >= tempChart.data?.xMax ?: Float.MIN_VALUE) {
            val calendar = Calendar.getInstance()
            to = calendar.time.time
            calendar.add(Calendar.HOUR, -24)
            from = calendar.time.time
            storedReadings = inputReadings.toMutableList()
        }

        val tempData: MutableList<Entry> = ArrayList()
        val humidData: MutableList<Entry> = ArrayList()
        val pressureData: MutableList<Entry> = ArrayList()

        storedReadings?.let { tagReadings ->
            if (tagReadings.size > 0) {
                from = tagReadings[0].createdAt.time

                val entries = tagReadings.map { entry->
                    GraphEntry(
                        timestamp = (entry.createdAt.time - from).toFloat(),
                        temperature = unitsConverter.getTemperatureValue(entry.temperature).toFloat(),
                        humidity = entry.humidity?.let {
                            unitsConverter.getHumidityValue(it, entry.temperature).toFloat()
                        },
                        pressure = entry.pressure?.let {
                            unitsConverter.getPressureValue(it).toFloat()
                        }
                    )
                }

                entries.forEach {entry ->
                    tempData.add(Entry(entry.timestamp, entry.temperature))
                    entry.humidity?.let {
                        humidData.add(Entry(entry.timestamp, it))
                    }
                    entry.pressure?.let {
                        pressureData.add(Entry(entry.timestamp, it))
                    }
                }
            } else {
                val timestamp = to.toFloat()
                tempData.add(Entry(timestamp, 0f))
                humidData.add(Entry(timestamp, 0f))
                pressureData.add(Entry(timestamp, 0f))
            }

            addDataToChart(tempData, tempChart, context.getString(R.string.temperature_with_unit, unitsConverter.getTemperatureUnitString()))
            addDataToChart(humidData, humidChart, context.getString(R.string.humidity_with_unit, unitsConverter.getHumidityUnitString()))
            addDataToChart(pressureData, pressureChart, context.getString(R.string.pressure_with_unit, unitsConverter.getPressureUnitString()))

            if (!offsetsNormalized) {
                normalizeOffsets(tempChart, humidChart, pressureChart)
                offsetsNormalized = true
            }
        }
    }

    private fun setupVisibility(view: View, showTemperature: Boolean, showHumidity: Boolean, showPressure: Boolean) {
        if (visibilitySet) return
        tempChart = view.findViewById(R.id.tempChart)
        humidChart = view.findViewById(R.id.humidChart)
        pressureChart = view.findViewById(R.id.pressureChart)

        Timber.d("setupVisibility $showTemperature $showHumidity $showPressure")

        tempChart.isVisible = showTemperature
        humidChart.isVisible = showHumidity
        pressureChart.isVisible = showPressure

        if (context.resources.configuration.orientation != ORIENTATION_LANDSCAPE) {
            view.findViewById<View>(R.id.spacerTop)?.isVisible = !showHumidity && !showPressure
            view.findViewById<View>(R.id.spacerBottom)?.isVisible = !showHumidity && !showPressure
        }

        visibilitySet = true
    }

    private fun setupCharts(view: View) {
        if (!graphSetupCompleted) {
            tempChart = view.findViewById(R.id.tempChart)
            humidChart = view.findViewById(R.id.humidChart)
            pressureChart = view.findViewById(R.id.pressureChart)

            tempChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.2f")
            tempChart.axisLeft.granularity = 0.01f
            humidChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.2f")
            humidChart.axisLeft.granularity = 0.01f
            if (unitsConverter.getPressureUnit() == PressureUnit.PA) {
                pressureChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.0f")
                pressureChart.axisLeft.granularity = 1f
            } else {
                pressureChart.axisLeft.valueFormatter = AxisLeftValueFormatter("%.2f")
                pressureChart.axisLeft.granularity = 0.01f
            }

            tempChart.axisRight.isEnabled = false
            humidChart.axisRight.isEnabled = false
            pressureChart.axisRight.isEnabled = false

            synchronizeChartGestures(setOf(tempChart, humidChart, pressureChart))
            graphSetupCompleted = true
        }
    }

    private fun addDataToChart(data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawCircles(preferencesRepository.graphDrawDots())
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.highLightColor = ContextCompat.getColor(context, R.color.main)
        set.circleRadius = 1f
        chart.setXAxisRenderer(
            CustomXAxisRenderer(
                chart.viewPortHandler,
                chart.xAxis,
                chart.getTransformer(YAxis.AxisDependency.LEFT)
            )
        )
        chart.xAxis.axisMaximum = (to - from).toFloat()
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setLabelCount(6, false)
        chart.axisLeft.setLabelCount(5, false)

        chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
        chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
        chart.axisLeft.isGranularityEnabled = true
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
        chart.axisLeft.axisMinimum = set.yMin - 0.5f
        chart.axisLeft.axisMaximum = set.yMax + 0.5f

        chart.legend.isEnabled = false
        chart.data = LineData(set)
        chart.data.isHighlightEnabled = false
        chart.xAxis.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, p1: AxisBase?): String {
                val date = Date(value.toLong() + from)
                val timeText = getTimeInstance(DateFormat.SHORT).format(date)

                val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
                val dateText: String = DateUtils.formatDateTime(context, value.toLong() + from, flags)

                return "$timeText\n$dateText"
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
        val offsetLeft =
            if (pressureChart.viewPortHandler.offsetLeft() > tempChart.viewPortHandler.offsetLeft()) {
                pressureChart.viewPortHandler.offsetLeft() * 1.1f
            } else {
                tempChart.viewPortHandler.offsetLeft() * 1.1f
            }
        val offsetBottom = pressureChart.viewPortHandler.offsetBottom() * 2f
        val offsetTop = pressureChart.viewPortHandler.offsetTop() / 2f
        val offsetRight = pressureChart.viewPortHandler.offsetRight() / 2f

        tempChart.setViewPortOffsets(
            offsetLeft,
            offsetTop,
            offsetRight,
            offsetBottom
        )

        humidChart.setViewPortOffsets(
            offsetLeft,
            offsetTop,
            offsetRight,
            offsetBottom
        )

        pressureChart.setViewPortOffsets(
            offsetLeft,
            offsetTop,
            offsetRight,
            offsetBottom
        )
    }

    class AxisLeftValueFormatter(private val formatPattern: String) : IAxisValueFormatter {
        override fun getFormattedValue(value: Float, p1: AxisBase?): String {
            return formatPattern.format(value)
        }
    }
}