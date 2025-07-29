package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartRanges
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerDimensions
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.Position
import com.ruuvi.station.app.ui.components.MarkupText
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.tagdetails.ui.ChartHistory
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.getDescriptionBodyResId
import com.ruuvi.station.util.extensions.isStartOfTheDay
import timber.log.Timber
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueBottomSheet (
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier,
    chartHistory: ChartHistory?,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        containerColor = RuuviStationTheme.colors.sensorValueBottomSheetBackground,
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.7f))
            )
        },
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        ValueSheetContent(sheetValue, chartHistory)
    }
}

@Composable
fun ValueSheetContent(
    sheetValue: EnvironmentValue,
    chartHistory: ChartHistory?
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val maxHeight = screenHeight * 0.75f

    Column (
        modifier = Modifier
            .heightIn(max = maxHeight)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
    ) {
        ValueSheetHeader(sheetValue)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        if (chartHistory != null && chartHistory.first.isNotEmpty()) {
            ChartFor2Days(chartHistory)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }
        MarkupText(sheetValue.unitType.getDescriptionBodyResId())
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
    }
}

@Composable
fun ChartFor2Days(
    chartHistory: ChartHistory,
    modifier: Modifier = Modifier
) {
    val minY = chartHistory.second.min()
    val maxY = chartHistory.second.max()

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries { series(chartHistory.first, chartHistory.second) }
        }
    }

    CartesianChartHost(
        chart =
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider =
                        LineCartesianLayer.LineProvider.series(
                            LineCartesianLayer.rememberLine(
                                fill = LineCartesianLayer.LineFill.single(fill(RuuviStationTheme.colors.chartLine)),
                                stroke = LineCartesianLayer.LineStroke.continuous(thickness = 1.dp)
                            )
                        ),
                    rangeProvider = CartesianLayerRangeProvider.fixed(minY = minY -1, maxY = maxY + 1)
                ),
                startAxis = VerticalAxis.rememberStart(itemPlacer = itemPlacerVertical),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = BottomAxisValueFormatter,
                    itemPlacer = itemPlacer
                ),
            ),
        modelProducer = modelProducer,
        zoomState = VicoZoomState(zoomEnabled = false, Zoom.Content, Zoom.Content, Zoom.Content),
        modifier = modifier,
    )

}

val itemPlacerVertical = object : VerticalAxis.ItemPlacer {
    override fun getBottomLayerMargin(
        context: CartesianMeasuringContext,
        verticalLabelPosition: Position.Vertical,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float = 5f

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        position: Axis.Position.Vertical
    ): List<Double> {
        val yRange = context.ranges.getYRange(position)
        return listOf(yRange.minY, (yRange.minY + yRange.maxY).roundHalfUp(0), yRange.maxY)
    }

    override fun getLabelValues(
        context: CartesianDrawingContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: Axis.Position.Vertical
    ): List<Double> {

        Timber.d("Y getLabelValues $maxLabelHeight $axisHeight")
        val yRange = context.ranges.getYRange(position)
        return getLabels(
            yRange = yRange,
            axisHeight = axisHeight,
            maxLabelHeight = maxLabelHeight
        )
    }

    fun getLabels(
        yRange: CartesianChartRanges.YRange,
        axisHeight: Float,
        maxLabelHeight: Float,
    ): List<Double> {
        val labelsCount = ceil(axisHeight / maxLabelHeight / 2)

        val range = yRange.maxY - yRange.minY
        val interval = getClosestPredefinedInterval(range, labelsCount.toInt())

        val positions = mutableListOf<Double>()

        val start = (yRange.minY.toLong() / interval) * interval

        var tick = start
        while (tick <= yRange.maxY) {
            if (tick > yRange.minY) {
                positions.add(tick)
            }
            tick += interval
        }
        return positions
    }

    override fun getTopLayerMargin(
        context: CartesianMeasuringContext,
        verticalLabelPosition: Position.Vertical,
        maxLabelHeight: Float,
        maxLineThickness: Float
    ): Float = 5f

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        axisHeight: Float,
        maxLabelHeight: Float,
        position: Axis.Position.Vertical
    ): List<Double> {
        val yRange = context.ranges.getYRange(position)
        return getLabels(
            yRange = yRange,
            axisHeight = axisHeight,
            maxLabelHeight = maxLabelHeight
        )
    }

    private fun getClosestPredefinedInterval(range: Double, labelCount: Int): Double {
        return intervals.sortedBy { abs(range / it - labelCount) }.first()
    }

    val intervals = doubleArrayOf(
        0.01,
        0.02,
        0.05,
        0.1,
        0.2,
        0.5,
        1.0,
        2.0,
        5.0,
        10.0,
        20.0,
        25.0,
        50.0,
        100.0,
        200.0,
        250.0,
        500.0,
        1000.0,
        2000.0,
        2500.0,
        5000.0,
        10000.0,
        20000.0,
        25000.0,
        50000.0,
        100000.0,
        200000.0,
        250000.0,
        500000.0,
        1000000.0
    )
}


private val BottomAxisValueFormatter =
    CartesianValueFormatter { context, value, verticalAxisPosition ->
        val date = Date(value.toLong())
        if (date.isStartOfTheDay()) {
//            val flags: Int = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
//            DateUtils.formatDateTime(context, date.time, flags)
            val formatter = SimpleDateFormat("dd.MM", Locale.getDefault())
            formatter.format(date)
        } else {
            DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ","")
        }
    }

val itemPlacer = object : HorizontalAxis.ItemPlacer {
    override fun getEndLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float
    ): Float = 0f

    val sixHoursMillis = 6 * 60 * 60 * 1000L

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float
    ): List<Double> {
        Timber.d("getHeightMeasurementLabelValues $maxLabelWidth")
        return listOf(fullXRange.start, fullXRange.endInclusive)
    }

    override fun getLabelValues(
        context: CartesianDrawingContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float
    ): List<Double> {
        Timber.d("getLabelValues $maxLabelWidth ${visibleXRange.start} ${visibleXRange.endInclusive}")

        val labelsCount = ceil(context.layerBounds.width() / maxLabelWidth / 2)
        val range = visibleXRange.endInclusive - visibleXRange.start
        val rawInterval = range / labelsCount
        val interval = getClosestPredefinedInterval(rawInterval.toDouble())

        val positions = mutableListOf<Double>()
        val xMin = visibleXRange.start
        val xMax = visibleXRange.endInclusive


        val start = (xMin.toLong() / interval) * interval

        val localOffset = if (interval > 3600000) TimeZone.getDefault().getOffset(start) else 0

        var tick = start- localOffset
        while (tick <= xMax) {
            positions.add(tick.toDouble())
            tick += interval
        }
        return positions
    }


    override fun getStartLayerMargin(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        tickThickness: Float,
        maxLabelWidth: Float
    ): Float = 0f

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasuringContext,
        layerDimensions: CartesianLayerDimensions,
        fullXRange: ClosedFloatingPointRange<Double>
    ): List<Double> {
        return listOf(fullXRange.start, fullXRange.endInclusive)
    }

    private fun getClosestPredefinedInterval(rawInterval: Double): Long {
        return intervals.sortedBy { abs(it - rawInterval) }.first()
    }


    val intervals = longArrayOf(
        60000,      // 1m
        120000,     // 2m
        180000,     // 3m
        300000,     // 5m
        600000,     // 10m
        900000,     // 15m
        1800000,    // 30m
        3600000,    // 1h
        7200000,    // 2h
        10800000,   // 3h
        21600000,   // 6h
        43200000,   // 12h
        86400000,   // 1d
        172800000,  // 2d
        345600000,  // 4d
        691200000,  // 8d
    )
}


@Composable
fun ValueSheetHeader(
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {

        Icon(
            modifier = Modifier
                .height(24.dp.scaleUpTo(1.5f))
                .padding(end = RuuviStationTheme.dimensions.medium),
            painter = painterResource(id = sheetValue.unitType.iconRes),
            tint = Color(0xff5ebdb2),
            contentDescription = ""
        )

        ValueSheetHeaderText(
            modifier = Modifier
                .alignByBaseline(),
            text = stringResource(sheetValue.unitType.measurementTitle)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ValueSheetHeaderText(
                modifier = Modifier
                    .alignByBaseline(),
                text = sheetValue.valueWithoutUnit
            )

            ValueSheetUnitText(
                modifier = Modifier
                    .alignByBaseline()
                    .padding(
                        start = RuuviStationTheme.dimensions.small
                    ),
                text = sheetValue.unitString
            )
        }
    }
}

@Composable
fun ValueSheetHeaderText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        fontSize = RuuviStationTheme.fontSizes.normal.limitScaleTo(1.5f),
        fontFamily = ruuviStationFonts.montserratBold,
        fontWeight = FontWeight.Bold,
        text = text,
        color = Color.White
    )
}

@Composable
fun ValueSheetUnitText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        style = RuuviStationTheme.typography.dashboardSecondary,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
        text = text,
        maxLines = 1
    )
}

@Preview
@Composable
private fun ValueBottomSheet() {
    val value = EnvironmentValue(
        original = 22.50,
        value = 22.50,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "22.5 %",
        valueWithoutUnit = "22.5",
        unitString = "%",
        unitType = UnitType.HumidityUnit.Relative
    )

    RuuviTheme {
        Surface(color = RuuviStationTheme.colors.sensorValueBottomSheetBackground) {
            ValueSheetContent(sheetValue = value, null)
        }
    }
}