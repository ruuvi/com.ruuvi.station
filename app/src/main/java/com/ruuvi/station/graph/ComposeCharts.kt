package com.ruuvi.station.graph

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.text.format.DateUtils
import android.view.MotionEvent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
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
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.graph.model.ChartSensorType
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.PressureUnit
import com.ruuvi.station.util.extensions.isStartOfTheDay
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.text.DateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChartsView(
    modifier: Modifier,
    sensorId: String,
    temperatureChart: LineChart,
    humidityChart: LineChart,
    pressureChart: LineChart,
    unitsConverter: UnitsConverter,
    selected: Boolean,
    chartCleared: Flow<String>,
    getHistory: (String) -> List<TagSensorReading>
) {
    Timber.d("ChartView - top $sensorId $selected")
    val context = LocalContext.current

    var history by remember {
        mutableStateOf<List<TagSensorReading>>(listOf())
    }

    var from by remember {
        mutableStateOf(0L)
    }

    var to by remember {
        mutableStateOf(0L)
    }

    var temperatureData by remember {
        mutableStateOf<MutableList<Entry>>(ArrayList())
    }
    var humidityData by remember {
        mutableStateOf<MutableList<Entry>>(ArrayList())
    }
    var pressureData by remember {
        mutableStateOf<MutableList<Entry>>(ArrayList())
    }

    LaunchedEffect(key1 = sensorId) {
        Timber.d("ChartView - initial setup $sensorId")
        normalizeOffsets(temperatureChart, humidityChart, pressureChart)
        synchronizeChartGestures(setOf(temperatureChart, humidityChart, pressureChart))
        setupHighLighting(setOf(temperatureChart, humidityChart, pressureChart))

        chartCleared.collect{
            Timber.d("ChartView - chart cleared $it")
            temperatureData.clear()
            humidityData.clear()
            pressureData.clear()
            temperatureChart.fitScreen()
            humidityChart.fitScreen()
            pressureChart.fitScreen()
        }
    }

    LaunchedEffect(key1 = selected) {
        Timber.d("ChartView - LaunchedEffect $sensorId")
        while (selected) {
            Timber.d("ChartView - get history $sensorId")
            val freshHistory = getHistory.invoke(sensorId)

            if (history.isEmpty() ||
                temperatureChart.highestVisibleX >= (temperatureChart.data?.xMax ?: Float.MIN_VALUE)) {
                history = freshHistory

                if (history.isNotEmpty()) {
                    Timber.d("ChartView - prepare datasets $sensorId pointsCount = ${history.size}")
                    from = history[0].createdAt.time
                    to = Date().time

                    val temperatureDataTemp = mutableListOf<Entry>()
                    val humidityDataTemp = mutableListOf<Entry>()
                    val pressureDataTemp = mutableListOf<Entry>()
                    history.forEach { item ->
                        val timestamp = (item.createdAt.time - from).toFloat()
                        temperatureDataTemp.add(Entry(timestamp, unitsConverter.getTemperatureValue(item.temperature).toFloat()))
                        item.humidity?.let { humidity ->
                            humidityDataTemp.add(Entry(timestamp, unitsConverter.getHumidityValue(humidity, item.temperature).toFloat()))
                        }
                        item.pressure?.let {pressure ->
                            pressureDataTemp.add(Entry(timestamp, unitsConverter.getPressureValue(pressure).toFloat()))
                        }
                    }
                    temperatureData = temperatureDataTemp
                    humidityData = humidityDataTemp
                    pressureData = pressureDataTemp
                }
            } else {

            }
            delay(1000)
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        val chartTypes = mutableListOf<ChartSensorType>(ChartSensorType.TEMPERATURE)
        if (humidityData.isNotEmpty()) chartTypes.add(ChartSensorType.HUMIDITY)
        if (pressureData.isNotEmpty()) chartTypes.add(ChartSensorType.PRESSURE)

        VerticalPager(
            modifier = modifier.fillMaxSize(),
            pageCount = chartTypes.size,
            beyondBoundsPageCount = 3
        ) {page ->
            val chartSensorType = chartTypes[page]

            when (chartSensorType) {
                ChartSensorType.TEMPERATURE -> {
                    ChartView(temperatureChart, Modifier.fillMaxSize(), temperatureData, unitsConverter, ChartSensorType.TEMPERATURE, from, to)
                }
                ChartSensorType.HUMIDITY -> {
                    ChartView(humidityChart, Modifier.fillMaxSize(), humidityData, unitsConverter, ChartSensorType.HUMIDITY, from, to)
                }
                ChartSensorType.PRESSURE -> {
                    ChartView(pressureChart, Modifier.fillMaxSize(), pressureData, unitsConverter, ChartSensorType.PRESSURE, from, to)
                }
            }
        }
    }
    else {
        Column(modifier = modifier.fillMaxSize()) {
            if (temperatureData.isEmpty() && humidityData.isEmpty() && pressureData.isEmpty()) {
                EmptyCharts()
            } else {
                val onlyOneChart = humidityData.isEmpty() && pressureData.isEmpty()
                if (onlyOneChart) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .weight(0.5f))
                }
                Box(modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)) {
                    ChartView(temperatureChart, Modifier.fillMaxSize(), temperatureData, unitsConverter, ChartSensorType.TEMPERATURE, from, to)
                }
                if (onlyOneChart) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .weight(0.5f))
                }
                if (humidityData.isNotEmpty()){
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)) {
                        ChartView(humidityChart, Modifier.fillMaxSize(), humidityData, unitsConverter, ChartSensorType.HUMIDITY, from, to)
                    }
                }
                if (pressureData.isNotEmpty()) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)) {
                        ChartView(pressureChart, Modifier.fillMaxSize(), pressureData, unitsConverter, ChartSensorType.PRESSURE, from, to)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCharts() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.extended),
            style = RuuviStationTheme.typography.onboardingSubtitle,
            text = stringResource(id = R.string.empty_chart_message),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChartView(
    lineChart: LineChart,
    modifier: Modifier,
    chartData: MutableList<Entry>,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType,
    from: Long,
    to: Long,
) {
    val context = LocalContext.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            Timber.d("ChartView - factory")
            val chart = lineChart
            setupChart(chart, unitsConverter, chartSensorType)
            applyChartStyle(
                context = context,
                chart = chart,
                unitsConverter = unitsConverter,
                chartSensorType = chartSensorType) {
                from
            }
            chart
        },
        update = { view ->
            Timber.d("ChartView - update $from pointsCount = ${chartData.size}")
            val latestPoint = chartData.lastOrNull()
            val chartCaption = if (latestPoint != null) {
                val latestValue = when (chartSensorType) {
                    ChartSensorType.TEMPERATURE -> unitsConverter.getTemperatureRawString(latestPoint.y.toDouble(), Accuracy.Accuracy2)
                    ChartSensorType.HUMIDITY -> unitsConverter.getHumidityRawString(latestPoint.y.toDouble(), Accuracy.Accuracy2)
                    ChartSensorType.PRESSURE -> unitsConverter.getPressureRawString(latestPoint.y.toDouble(), Accuracy.Accuracy2)
                }
                context.getString(chartSensorType.captionTemplate, latestValue)
            } else {
                context.getString(chartSensorType.captionTemplate, "")
            }
            addDataToChart(context, chartData, view, chartCaption, from, to)
        }
    )
}

fun setupChart(
    chart: LineChart,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType
) {
    if (chartSensorType == ChartSensorType.TEMPERATURE || chartSensorType == ChartSensorType.HUMIDITY) {
        chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#.##")
        chart.axisLeft.granularity = 0.01f
    } else {
        if (unitsConverter.getPressureUnit() == PressureUnit.PA) {
            chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#")
            chart.axisLeft.granularity = 1f
        } else {
            chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#.##")
            chart.axisLeft.granularity = 0.01f
        }
    }
}

private fun applyChartStyle(
    context: Context,
    chart: LineChart,
    unitsConverter: UnitsConverter,
    chartSensorType: ChartSensorType,
    getFrom: () -> Long
) {
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
        unitsConverter = unitsConverter,
        getFrom = getFrom
    )
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

private fun addDataToChart(
    context: Context,
    data: MutableList<Entry>,
    chart: LineChart,
    label: String,
    from: Long,
    to: Long
) {
    val set = LineDataSet(data, label)
    setLabelCount(chart)
    //set.setDrawCircles(preferencesRepository.graphDrawDots())
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

    chart.description.text = label
    chart.axisLeft.axisMinimum = set.yMin - 1f
    chart.axisLeft.axisMaximum = set.yMax + 1f
    chart.axisLeft.setDrawTopYLabelEntry(false)

    chart.data = LineData(set)
    chart.data.isHighlightEnabled = true
    chart.xAxis.valueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Double, p1: AxisBase?): String {
            val date = Date(value.toLong() + from)
            return if (date.isStartOfTheDay()) {
                val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
                DateUtils.formatDateTime(context, date.time, flags)
            } else {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ","")
            }
        }
    }
    chart.notifyDataSetChanged()
    chart.invalidate()
}

private fun setLabelCount(chart: LineChart) {
    val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date())

    val computePaint = Paint(1)
    computePaint.typeface = chart.xAxis.typeface
    computePaint.textSize = chart.xAxis.textSize
    val computeSize = Utils.calcTextSize(computePaint, timeText)

    Timber.d("setLabelCount computeLabelWidth = $computeSize contentWidth = ${chart.viewPortHandler.contentWidth()}")

    var labelCount = chart.viewPortHandler.contentWidth() / (computeSize.width * 1.7)
    chart.xAxis.setLabelCount(labelCount.toInt(), false)
    chart.axisLeft.setLabelCount(6, false)
}

// Manually setting offsets to be sure that all of the charts have equal offsets. This is needed for synchronous zoom and dragging.
private fun normalizeOffsets(temperatureChart: LineChart, humidityChart: LineChart, pressureChart: LineChart) {
    val computePaint = Paint(1)
    computePaint.typeface = pressureChart.axisLeft.typeface
    computePaint.textSize = pressureChart.axisLeft.textSize
    val computeSize = Utils.calcTextSize(computePaint, "0000.00")
    val computeHeight = Utils.calcTextHeight(computePaint, "Q").toFloat()

    val offsetLeft = computeSize.width * 1.1f
    val offsetBottom = computeHeight * 2
    val offsetTop = offsetBottom / 2f
    val offsetRight = offsetBottom / 2f

    Timber.d("Offsets top = $offsetTop bottom = $offsetBottom left = $offsetLeft right = $offsetRight computeSize = $computeSize computeHeight = $computeHeight")

    temperatureChart.setViewPortOffsets(
        offsetLeft,
        offsetTop,
        offsetRight,
        offsetBottom
    )

    humidityChart.setViewPortOffsets(
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

private fun setupHighLighting(charts: Set<LineChart>) {
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