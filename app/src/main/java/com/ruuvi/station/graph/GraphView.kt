package com.ruuvi.station.graph

import android.content.Context
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
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
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.utils.Utils
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.GraphEntry
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.util.extensions.isStartOfTheDay
import timber.log.Timber
import java.text.DateFormat
import java.text.DateFormat.getTimeInstance
import java.text.DecimalFormat
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
    private val isTablet = context.resources.getBoolean(R.bool.isTablet)

    private lateinit var tempChart: LineChart
    private lateinit var humidChart: LineChart
    private lateinit var pressureChart: LineChart

    fun drawChart(
            inputReadings: List<TagSensorReading>,
            view: View
    ) {
        Timber.d("drawChart pointsCount = ${inputReadings.size} isTablet $isTablet")
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

            val latest = tagReadings.lastOrNull()

            val temperatureLast = if (latest != null) unitsConverter.getTemperatureString(latest.temperature) else unitsConverter.getTemperatureUnitString()
            val humidityLast = if (latest != null) unitsConverter.getHumidityString(latest.humidity, latest.temperature) else unitsConverter.getHumidityUnitString()
            val pressureLast = if (latest != null) unitsConverter.getPressureString(latest.pressure) else unitsConverter.getPressureUnitString()

            addDataToChart(tempData, tempChart, context.getString(R.string.temperature_with_unit, temperatureLast))
            addDataToChart(humidData, humidChart, context.getString(R.string.humidity_with_unit, humidityLast))
            addDataToChart(pressureData, pressureChart, context.getString(R.string.pressure_with_unit, pressureLast))

            if (!offsetsNormalized) {
                normalizeOffsets(tempChart, humidChart, pressureChart)
                offsetsNormalized = true
            }
        }
    }

    fun clearView() {
        storedReadings = null
        tempChart.clear()
        humidChart.clear()
        pressureChart.clear()
        tempChart.fitScreen()
        humidChart.fitScreen()
        pressureChart.fitScreen()
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

            tempChart.axisLeft.valueFormatter = AxisLeftValueFormatter("#.##")
            tempChart.axisLeft.granularity = 0.01f
            humidChart.axisLeft.valueFormatter = AxisLeftValueFormatter("#.##")
            humidChart.axisLeft.granularity = 0.01f
            if (unitsConverter.getPressureUnit() == PressureUnit.PA) {
                pressureChart.axisLeft.valueFormatter = AxisLeftValueFormatter("#")
                pressureChart.axisLeft.granularity = 1f
            } else {
                pressureChart.axisLeft.valueFormatter = AxisLeftValueFormatter("#.##")
                pressureChart.axisLeft.granularity = 0.01f
            }
            synchronizeChartGestures(setOf(tempChart, humidChart, pressureChart))
            graphSetupCompleted = true

            applyChartStyle(tempChart, ChartSensorType.TEMPERATURE)
            applyChartStyle(humidChart, ChartSensorType.HUMIDITY)
            applyChartStyle(pressureChart, ChartSensorType.PRESSURE)

            setupHighLighting(arrayListOf(tempChart, humidChart, pressureChart))
        }
    }

    private fun setupHighLighting(charts: ArrayList<LineChart>) {
        for (chart in charts) {
            val otherCharts = charts.filter { it != chart }
            chart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(entry: Entry, highlight: Highlight) {
                    for (otherChart in otherCharts) {
                        otherChart.highlightValue(entry.x, highlight.dataSetIndex, false)
                    }
                }

                override fun onNothingSelected() {
                    for (otherChart in otherCharts) {
                        otherChart.highlightValue(0f, -1, false)
                    }
                }
            })
        }
    }

    private fun applyChartStyle(chart: LineChart, chartSensorType: ChartSensorType) {
        chart.axisRight.isEnabled = false

        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM

        chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
        chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
        chart.axisLeft.isGranularityEnabled = true
        chart.description.textColor = Color.WHITE
        chart.description.yOffset = 5f
        chart.description.xOffset = 5f
        chart.dragDecelerationFrictionCoef = 0.8f
        chart.setNoDataTextColor(Color.WHITE)
        chart.viewPortHandler.setMaximumScaleX(5000f)
        chart.viewPortHandler.setMaximumScaleY(30f)
        chart.setTouchEnabled(true)
        chart.isDoubleTapToZoomEnabled = false
        chart.isHighlightPerTapEnabled = true

        val markerView = ChartMarkerView(
            context = context,
            layoutResource = R.layout.custom_marker_view,
            chartSensorType = chartSensorType,
            unitsConverter = unitsConverter
        ) {
            return@ChartMarkerView from
        }
        markerView.chartView = chart
        chart.marker = markerView

        try {
            val font = ResourcesCompat.getFont(context, R.font.mulish_regular)
            chart.description.typeface = font
            chart.axisLeft.typeface = font
            chart.xAxis.typeface = font
        } catch (e: Exception) {
            Timber.e(e)
        }

        val textSize = context.resources.getDimension(R.dimen.graph_description_size)
        chart.description.textSize = textSize
        chart.axisLeft.textSize = textSize
        chart.xAxis.textSize = textSize
        chart.legend.isEnabled = false
    }


    private fun addDataToChart(data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawCircles(preferencesRepository.graphDrawDots())
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.circleRadius = 1f
        set.color = ContextCompat.getColor(context, R.color.chartLineColor)
        set.setCircleColor(ContextCompat.getColor(context, R.color.chartLineColor))
        set.fillColor = ContextCompat.getColor(context, R.color.chartFillColor)
        chart.setXAxisRenderer(
            CustomXAxisRenderer(
                from,
                chart.viewPortHandler,
                chart.xAxis,
                chart.getTransformer(YAxis.AxisDependency.LEFT)
            )
        )
        chart.rendererLeftYAxis = CustomYAxisRenderer(
            chart.viewPortHandler,
            chart.axisLeft,
            chart.getTransformer(YAxis.AxisDependency.LEFT)
        )
        set.enableDashedHighlightLine(10f, 5f, 0f)
        set.setDrawHighlightIndicators(true)
        set.highLightColor = ContextCompat.getColor(context, R.color.chartLineColor)

        chart.xAxis.axisMaximum = (to - from).toFloat()
        chart.xAxis.axisMinimum = 0f
        setLabelCount(chart)

        chart.description.text = label
        chart.axisLeft.axisMinimum = set.yMin - 0.5f
        chart.axisLeft.axisMaximum = set.yMax + 0.5f

        chart.data = LineData(set)
        chart.data.isHighlightEnabled = true
        chart.xAxis.valueFormatter = object : IAxisValueFormatter {
            override fun getFormattedValue(value: Double, p1: AxisBase?): String {
                val date = Date(value.toLong() + from)
                return if (date.isStartOfTheDay()) {
                    val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
                    DateUtils.formatDateTime(context, date.time, flags)
                } else {
                    getTimeInstance(DateFormat.SHORT).format(date).replace(" ","")
                }
            }
        }
        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    private fun setLabelCount(chart: LineChart) {
        val timeText = getTimeInstance(DateFormat.SHORT).format(Date())
        var labelCount = if (timeText.length > 5) 4 else 6
        if (isTablet) labelCount += 1
        chart.xAxis.setLabelCount(labelCount, false)
        chart.axisLeft.setLabelCount(6, false)
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

    // Manually setting offsets to be sure that all of the charts have equal offsets. This is needed for synchronous zoom and dragging.
    private fun normalizeOffsets(tempChart: LineChart, humidChart: LineChart, pressureChart: LineChart) {
        val computePaint = Paint(1)
        computePaint.typeface = pressureChart.axisLeft.typeface
        computePaint.textSize = pressureChart.axisLeft.textSize
        val computeSize = Utils.calcTextSize(computePaint, "0000.00")
        val computeHeight = Utils.calcTextHeight(computePaint, "Q").toFloat()

        val offsetLeft = computeSize.width * 1.1f
        val offsetBottom = computeHeight * 2
        val offsetTop = tempChart.viewPortHandler.offsetTop() / 2f
        val offsetRight = tempChart.viewPortHandler.offsetRight() / 2f

        Timber.d("Offsets top = $offsetTop bottom = $offsetBottom left = $offsetLeft right = $offsetRight computeSize = $computeSize computeHeight = $computeHeight")

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
        override fun getFormattedValue(value: Double, p1: AxisBase?): String {
            val decimalFormat = DecimalFormat(formatPattern)
            return decimalFormat.format(value)
        }
    }
}