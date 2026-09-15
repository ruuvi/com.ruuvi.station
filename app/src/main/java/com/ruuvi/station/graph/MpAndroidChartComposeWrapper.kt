package com.ruuvi.station.graph

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.text.format.DateUtils
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.size
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.utils.Utils
import com.ruuvi.station.history.HistoryPoint
import com.ruuvi.station.history.HistoryStatistics
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.components.scaledToMax
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.White50
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.tutorials.Tutorial
import com.ruuvi.station.tutorials.ui.TutorialDialog
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.extensions.isStartOfTheDay
import timber.log.Timber
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Date
import kotlin.math.max

private typealias RawValueFormatter = (Double) -> String

private fun UnitsConverter.rawValueFormatter(unitType: UnitType): RawValueFormatter {
    return when (unitType) {
        is UnitType.TemperatureUnit -> { value -> getTemperatureRawWithoutUnitString(value, null) }
        is UnitType.HumidityUnit -> { value -> getHumidityRawWithoutUnitString(value, getHumidityAccuracy(unitType)) }
        is UnitType.PressureUnit -> { value -> getPressureRawWithoutUnitString(value, null) }
        is UnitType.BatteryVoltageUnit -> { value -> getValueWithoutUnit(value, getVoltageAccuracy()) }
        is UnitType.Acceleration -> { value -> getValueWithoutUnit(value, getAccelerationAccuracy()) }
        is UnitType.PM -> { value -> getValueWithoutUnit(value, getPmAccuracy()) }
        is UnitType.CO2 -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.VOC -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.NOX -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.SignalStrengthUnit -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.AirQuality -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.Luminosity -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.SoundAvg -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.SoundPeak -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.MovementUnit -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
        is UnitType.MsnUnit -> { value -> getValueWithoutUnit(value, unitType.defaultAccuracy) }
    }
}

private fun LineChart.highlightAtTouch(eventX: Float, eventY: Float, sharedX: MutableState<Float?>) {
    getHighlightByTouchPoint(eventX, eventY)?.let { highlight ->
        highlightValue(highlight, false)
        sharedX.value = highlight.x
    }
}

private fun LineChart.applySharedHighlight(x: Float?) {
    val nearest = if (x == null) null else data?.dataSets?.mapIndexedNotNull { index, set ->
        set.getEntryForXValue(x, Float.NaN)?.let { index to it }
    }?.minByOrNull { kotlin.math.abs(it.second.x - x) }
    if (nearest == null) highlightValue(null, false)
    else highlightValue(nearest.second.x, nearest.first, false)
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ChartViewPrototype(
    lineChart: LineChart,
    modifier: Modifier,
    chartData: MutableList<Entry>,
    unitsConverter: UnitsConverter,
    unitType: UnitType,
    graphDrawDots: Boolean,
    showChartStats: Boolean,
    limits: Pair<Double,Double>?,
    from: Long,
    to: Long,
    sharedX: MutableState<Float?>,
    statistics: HistoryStatistics?,
    axisStart: Long = from,
) {
    val context = LocalContext.current
    val title = stringResource(id = unitType.measurementName).substringBefore(" (")
    val icon = unitType.iconRes
    val description = remember(statistics, unitsConverter, unitType) {
        getChartDescription(context, statistics, unitsConverter, unitType)
    }
    val rendered = remember(lineChart) { arrayOfNulls<Any>(1) }
    val renderKey = listOf(chartData, from, to, axisStart, graphDrawDots, limits)
    val getRawValue = remember(unitsConverter, unitType) { unitsConverter.rawValueFormatter(unitType) }

    val latestValue = statistics?.latest?.let(getRawValue) ?: ""

    Column (
        modifier = modifier
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = RuuviStationTheme.dimensions.screenPadding,
                    end = RuuviStationTheme.dimensions.screenPadding,
                    top = RuuviStationTheme.dimensions.medium
                ),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .height(20.dp.scaleUpTo(1.5f))
                    .padding(top = RuuviStationTheme.dimensions.tiny, end = RuuviStationTheme.dimensions.medium),
                tint = RuuviStationTheme.colors.measurementIcon
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = RuuviStationTheme.dimensions.tiny)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        fontFamily = ruuviStationFonts.mulishBold,
                        fontSize = RuuviStationTheme.fontSizes.small.scaledToMax(max = 20.sp),
                        text = title,
                        color = RuuviStationTheme.colors.buttonText
                    )

                    Text(
                        fontFamily = ruuviStationFonts.mulishBold,
                        fontSize = RuuviStationTheme.fontSizes.small.scaledToMax(max = 20.sp),
                        text = if (unitType.measurementCode == "AQI") "$latestValue/100" else latestValue,
                        color = RuuviStationTheme.colors.buttonText
                    )

                    Text(
                        modifier = Modifier.padding(
                            start = RuuviStationTheme.dimensions.small
                        ),
                        fontFamily = ruuviStationFonts.mulishBold,
                        fontSize = RuuviStationTheme.fontSizes.small.scaledToMax(max = 20.sp),
                        text = stringResource(unitType.unit),
                        color = RuuviStationTheme.colors.buttonText
                    )
                }

                if (showChartStats) {
                    Text(
                        modifier = Modifier.padding(
                            top = RuuviStationTheme.dimensions.tiny,
                            bottom = RuuviStationTheme.dimensions.small
                        ),
                        color = White50,
                        fontFamily = ruuviStationFonts.mulishRegular,
                        fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                        text = description,
                    )
                }
            }
        }

        var chartTapped by rememberSaveable { mutableStateOf(false) }
        TutorialDialog(
            tutorial = Tutorial.ChartActionTutorial,
            showThisSession = chartTapped,
            onShowThisSessionChange = { chartTapped = it }
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .onGloballyPositioned {
                    Timber.d("setLabelCount onGloballyPositioned")
                    setLabelCount(context, lineChart)
                }
                .padding(horizontal = RuuviStationTheme.dimensions.medium),
            factory = { context ->
                Timber.d("ChartView AndroidView - factory")
                var markerDismissed = false

                val chart = lineChart
                setupMarker(
                    context = context,
                    chart = chart,
                    unitsConverter = unitsConverter,
                    unitType = unitType,
                    clearMarker = {
                        sharedX.value = null
                        markerDismissed = true
                    },
                    getFrom = { from }
                )

                var longPressActive = false

                val detector = GestureDetector(context,
                    object : GestureDetector.SimpleOnGestureListener() {
                        override fun onDown(e: MotionEvent): Boolean {
                            longPressActive = false
                            return true
                        }

                        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                            if (!markerDismissed) {
                                chartTapped = true
                            }
                            markerDismissed = false
                            return super.onSingleTapConfirmed(e)
                        }

                        override fun onLongPress(e: MotionEvent) {
                            longPressActive = true
                            chart.parent?.requestDisallowInterceptTouchEvent(true)
                            chart.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                            chart.getHighlightByTouchPoint(e.x, e.y)?.let { h ->
                                chart.highlightValue(h, false)
                                sharedX.value = h.x
                            }
                        }
                    }
                )

                chart.setOnTouchListener { v, event ->
                    detector.onTouchEvent(event)

                    when (event.actionMasked) {
                        MotionEvent.ACTION_MOVE -> {
                            if (longPressActive) {

                                v.parent?.requestDisallowInterceptTouchEvent(true)

                                chart.getHighlightByTouchPoint(event.x, event.y)?.let { h ->
                                    chart.highlightValue(h, false)
                                    sharedX.value = h.x
                                }
                                return@setOnTouchListener true
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressActive = false
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        MotionEvent.ACTION_DOWN -> {
                            longPressActive = false
                        }
                    }

                    longPressActive
                }

                chart
            },
            update = { view ->
                Timber.d("ChartView AndroidView - update $from pointsCount = ${chartData.size}")

                if (view.data == null || rendered[0] != renderKey) {
                    val matrix = Matrix(view.viewPortHandler.matrixTouch)
                    addDataToChart(context, chartData, view, "", graphDrawDots, limits, from, to, axisStart)
                    view.viewPortHandler.refresh(matrix, view, false)
                    (view.marker as ChartMarkerView).getFrom = { from }
                    rendered[0] = renderKey
                }
                view.applySharedHighlight(sharedX.value)

            }
        )
    }
}

fun getChartDescription(
    context: Context,
    statistics: HistoryStatistics?,
    unitsConverter: UnitsConverter,
    unitType: UnitType
): String {
    if (statistics == null) return ""
    val formatter = unitsConverter.rawValueFormatter(unitType)
    return context.getString(R.string.chart_min_max_avg,
        formatter(statistics.minimum), formatter(statistics.maximum), formatter(statistics.average))
}

fun chartsInitialSetup(
    context: Context,
    unitsConverter: UnitsConverter,
    charts: List<Pair<UnitType, LineChart>>,
    onViewportChanged: (LineChart, Boolean) -> Unit = { _, _ -> }
) {
    for (chartPair in charts) {
        setupChart(chartPair.second, unitsConverter, chartPair.first)
        applyChartStyle(
            context = context,
            chart = chartPair.second,
        )
    }

    normalizeOffsets(charts)
    synchronizeChartGestures(charts.map { it.second }.toSet(), onViewportChanged)
}


fun applyChartStyle(
    context: Context,
    chart: LineChart
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
    chart.xAxis.gridColor = ColorUtils.setAlphaComponent(chart.xAxis.gridColor, 100)
    chart.axisLeft.gridColor = ColorUtils.setAlphaComponent(chart.xAxis.gridColor, 100)
    chart.isDoubleTapToZoomEnabled = false
    chart.isHighlightPerTapEnabled = false
    chart.isHighlightPerDragEnabled = false

    try {
        val font = ResourcesCompat.getFont(context, R.font.mulish_regular)
        chart.description.typeface = font
        chart.axisLeft.typeface = font
        chart.xAxis.typeface = font
    } catch (e: Exception) {
        Timber.e(e)
    }

    val fontSize = if (context.resources.getBoolean(R.bool.isTablet)) 14.sp else 10.sp
    val sizeInPx = context.spToDpRespectingFontScale(fontSize.value)

    chart.description.textSize = sizeInPx
    chart.axisLeft.textSize = sizeInPx
    chart.xAxis.textSize = sizeInPx
    chart.legend.isEnabled = false
}

fun Context.spToDpRespectingFontScale(sp: Float): Float {
    val dm = resources.displayMetrics
    val px = sp * dm.scaledDensity          // honors Settings > Font size
    return px / dm.density                  // convert px -> dp for MPAndroidChart
}

fun setupMarker(
    context: Context,
    chart: LineChart,
    unitsConverter: UnitsConverter,
    unitType: UnitType,
    getFrom: () -> Long,
    clearMarker: () -> Unit
) {
    Timber.d("ChartView - setupMarker")
    val markerView = ChartMarkerView(
        context = context,
        layoutResource = R.layout.custom_marker_view,
        unitType = unitType,
        unitsConverter = unitsConverter,
        getFrom = getFrom,
        clearMarker = clearMarker
    )
    markerView.chartView = chart
    chart.marker = markerView
}

private fun addDataToChart(
    context: Context,
    data: MutableList<Entry>,
    chart: LineChart,
    label: String,
    graphDrawDots: Boolean,
    limits: Pair<Double,Double>?,
    from: Long,
    to: Long,
    axisStart: Long
) {
    Timber.d("ChartView - addDataToChart")
    val sets = historySegments(data).map { segment ->
        val set = LineDataSet(segment, label)
        set.setDrawCircles(graphDrawDots || segment.size == 1)
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.maximumGapBetweenPoints = Float.MAX_VALUE
        set.lineWidth = 1f
        set.circleRadius = 1.5f
        set.color = ContextCompat.getColor(context, R.color.chartLineColor)
        set.setCircleColor(ContextCompat.getColor(context, R.color.chartLineColor))
        set.setDrawCircleHole(false)
        set.fillColor = ContextCompat.getColor(context, R.color.chartFillColor)
        set.enableDashedHighlightLine(10f, 5f, 0f)
        set.setDrawHighlightIndicators(true)
        set.highLightColor = ContextCompat.getColor(context, R.color.chartLineColor)

        set
    }
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
    chart.xAxis.axisMaximum = (to - from).toFloat()
    chart.xAxis.axisMinimum = (axisStart - from).toFloat()

    chart.axisLeft.removeAllLimitLines()
    if (limits != null) {
        chart.axisLeft.addLimitLine(getLimitLine(context, limits.first.toFloat()))
        chart.axisLeft.addLimitLine(getLimitLine(context, limits.second.toFloat()))
    }

    chart.description.text = label
    if (data.isNotEmpty()) {
        chart.axisLeft.axisMinimum = data.minOf { it.y } - 1f
        chart.axisLeft.axisMaximum = data.maxOf { it.y } + 1f
    }
    chart.axisLeft.setDrawTopYLabelEntry(false)
    chart.axisLeft.valueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(p0: Double, p1: AxisBase?): String {
            return formatDoubleToString(p0)
        }
    }

    chart.data = LineData(sets)
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
    setLabelCount(context, chart)
}

fun getLimitLine(
    context: Context,
    value: Float
) : LimitLine {
    val limitLine = LimitLine(value)
    limitLine.lineColor = context.getColor(R.color.activeAlarm)
    limitLine.lineWidth = 1.5f
    return limitLine
}

fun formatDoubleToString(value: Double): String {
    val symbols = DecimalFormatSymbols.getInstance()
    val decimalFormat = DecimalFormat("#,##0.##", symbols)
    return decimalFormat.format(value)
}

private fun setLabelCount(context: Context, chart: LineChart) {
    val now = Date()
    val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(now)
    val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
    val dateText = DateUtils.formatDateTime(context, now.time, flags)

    val computePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    computePaint.typeface = chart.xAxis.typeface
    computePaint.textSize = chart.xAxis.textSize
    val computeSize = Utils.calcTextSize(computePaint, timeText)
    val computeSize2 = Utils.calcTextSize(computePaint, dateText)
    val width = max(computeSize.width, computeSize2.width)

    val labelCount = chart.viewPortHandler.contentWidth() / (width * 2)
    val labelCountY = chart.viewPortHandler.contentHeight() / (computeSize.height * 3.5)
    Timber.d("setLabelCount ${chart.size} VIEWPORT ${chart.viewPortHandler.contentWidth()} x ${chart.viewPortHandler.contentHeight()} x = $labelCount y = $labelCountY")
    chart.xAxis.setLabelCount(labelCount.toInt(), false)
    chart.axisLeft.setLabelCount(labelCountY.toInt(), false)
    chart.notifyDataSetChanged()
    chart.invalidate()
}

// Manually setting offsets to be sure that all of the charts have equal offsets. This is needed for synchronous zoom and dragging.
fun normalizeOffsets(charts: List<Pair<UnitType, LineChart>>) {
    if (charts.isEmpty()) return

    val computePaint = Paint(1)
    computePaint.typeface = charts.first().second.axisLeft.typeface
    computePaint.textSize = charts.first().second.axisLeft.textSize
    val computeSize = Utils.calcTextSize(computePaint, "0,000.00")
    val computeHeight = Utils.calcTextHeight(computePaint, "Q").toFloat()

    val offsetLeft = computeSize.width * 1.1f
    val offsetBottom = computeHeight * 2
    val offsetTop = offsetBottom / 2f
    val offsetRight = offsetBottom / 2f

    Timber.d("Offsets top = $offsetTop bottom = $offsetBottom left = $offsetLeft right = $offsetRight computeSize = $computeSize computeHeight = $computeHeight")

    for (chart in charts) {
        chart.second.setViewPortOffsets(
            offsetLeft,
            offsetTop,
            offsetRight,
            offsetBottom
        )
    }
}

fun synchronizeChartGestures(
    charts: Set<LineChart>, onViewportChanged: (LineChart, Boolean) -> Unit = { _, _ -> }
) {
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
                onViewportChanged(chart, true)
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
                onViewportChanged(chart, false)
            }

            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartDoubleTapped(me: MotionEvent?) {}
            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                synchronizeCharts(chart)
                onViewportChanged(chart, false)
            }
        }
    }
}

/** Segment ids come from raw measurements, never from distances between sampled points. */
internal fun historySegments(data: List<Entry>): List<List<Entry>> =
    data.groupBy { (it.data as? HistoryPoint)?.segment ?: 0 }.values.toList()
