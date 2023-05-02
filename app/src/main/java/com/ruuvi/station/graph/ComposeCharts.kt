package com.ruuvi.station.graph

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.text.format.DateUtils
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.github.mikephil.charting.utils.Utils
import com.ruuvi.station.R
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.isStartOfTheDay
import kotlinx.coroutines.delay
import timber.log.Timber
import java.text.DateFormat
import java.util.*

@Composable
fun ChartView(
    sensorId: String,
    unitsConverter: UnitsConverter,
    getHistory: (String) -> List<TagSensorReading>
) {
    var history by remember {
        mutableStateOf<List<TagSensorReading>>(listOf())
    }
    var from: Long = 0

    val context = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            Timber.d("ChartView - factory")
            val chart = LineChart(context)
            setupChart(chart)
            applyChartStyle(
                context = context,
                chart = chart,
                unitsConverter = unitsConverter,
                chartSensorType = ChartSensorType.TEMPERATURE) {
                from
            }
            chart
        },
        update = { view ->

            Timber.d("ChartView - update")
            val tempData: MutableList<Entry> = ArrayList()
            if (history.isNotEmpty()) {
                from = history[0].createdAt.time
                history.forEach { item ->
                    val timestamp = (item.createdAt.time - from).toFloat()
                    tempData.add(Entry(timestamp, item.temperature.toFloat()))
                }

                addDataToChart(context, tempData, view, "temp")
            }
        }
    )

    LaunchedEffect(key1 = Unit) {
        while (true) {
            Timber.d("ChartView - get history $sensorId")
            history = getHistory.invoke(sensorId)
            delay(1000)
        }
    }
}

fun setupChart(chart: LineChart) {

    chart.axisLeft.valueFormatter = GraphView.AxisLeftValueFormatter("#.##")
    chart.axisLeft.granularity = 0.01f
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
    label: String
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
            0,//from,
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

    //chart.xAxis.axisMaximum = (to - from).toFloat()
    chart.xAxis.axisMinimum = 0f

    chart.description.text = label
    chart.axisLeft.axisMinimum = set.yMin - 1f
    chart.axisLeft.axisMaximum = set.yMax + 1f
    chart.axisLeft.setDrawTopYLabelEntry(false)

    chart.data = LineData(set)
    chart.data.isHighlightEnabled = true
    chart.xAxis.valueFormatter = object : IAxisValueFormatter {
        override fun getFormattedValue(value: Double, p1: AxisBase?): String {
            val date = Date(value.toLong() + 0)//from)
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