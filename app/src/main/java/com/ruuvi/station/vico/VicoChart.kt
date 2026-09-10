package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.compose.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisTickComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Position
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.SegmentType

@Composable
fun VicoChartNoInteraction(
    chartHistory: ChartData,
    minMaxLocked: Pair<Double, Double>? = null,
    yAxisValues: List<Float>? = null,
    modifier: Modifier = Modifier
) {
    val minY = minMaxLocked?.first ?: chartHistory.minValue
    val maxY = minMaxLocked?.second ?: chartHistory.maxValue
    val modelProducer = remember { CartesianChartModelProducer() }

    var lineStyles by remember { mutableStateOf<List<LineCartesianLayer.Line>>(emptyList()) }

    val solidLine =
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(RuuviStationTheme.colors.chartLine)),
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 1.3.dp)
        )

    val pointComponent = rememberShapeComponent(
        fill = Fill(RuuviStationTheme.colors.chartLine),
        shape = CircleShape,
    )
    val pointsOnlyLine = LineCartesianLayer.rememberLine(
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 0.dp),
            pointProvider = LineCartesianLayer.PointProvider.single(
                point = LineCartesianLayer.Point(component = pointComponent, size = 1.3.dp)
            )
        )

    val dottedLine =
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(
                Fill(RuuviStationTheme.colors.chartLine)
            ),
            stroke = LineCartesianLayer.LineStroke.Dashed(
                thickness = 1.3.dp,
                dashLength = 1.dp,
                gapLength = 1.8.dp
            )
        )

    LaunchedEffect(chartHistory) {
        if (chartHistory.segments.isEmpty()) {
            lineStyles = emptyList()
            modelProducer.runTransaction { lineModel {  } }
            return@LaunchedEffect
        }

        // 2) Update styles to match segment order
        lineStyles = chartHistory.segments.map {
            when (it.segmentType) {
                SegmentType.Dotted -> dottedLine
                SegmentType.Single -> pointsOnlyLine
                SegmentType.Solid -> solidLine
            }
        }
        // 3) Feed the series to the model (order must match lineStyles)
        modelProducer.runTransaction {
            lineModel {
                chartHistory.segments.forEach { seg ->
                    series(seg.timestamps, seg.values)
                }
            }
        }
    }

    val customYAxisItemPlacer = remember(yAxisValues) {
        yAxisValues?.distinct()?.sorted()?.map { it.toDouble() }?.let { values ->
            object : VerticalAxis.ItemPlacer {
                override fun getHeightMeasurementLabelValues(
                    context: CartesianMeasuringContext,
                    position: Axis.Position.Vertical
                ) = values

                override fun getWidthMeasurementLabelValues(
                    context: CartesianMeasuringContext,
                    axisHeight: Float,
                    maxLabelHeight: Float,
                    position: Axis.Position.Vertical
                ) = values

                override fun getLabelValues(
                    context: CartesianDrawingContext,
                    axisHeight: Float,
                    maxLabelHeight: Float,
                    position: Axis.Position.Vertical
                ) = values

                override fun getLineValues(
                    context: CartesianDrawingContext,
                    axisHeight: Float,
                    maxLabelHeight: Float,
                    position: Axis.Position.Vertical
                ) = values // guidelines & ticks exactly at these values

                override fun getShiftTopLines(context: CartesianDrawingContext) = false

                override fun getTopLayerMargin(
                    context: CartesianMeasuringContext,
                    verticalLabelPosition: Position.Vertical,
                    maxLabelHeight: Float,
                    maxLineThickness: Float
                ) = 0f

                override fun getBottomLayerMargin(
                    context: CartesianMeasuringContext,
                    verticalLabelPosition: Position.Vertical,
                    maxLabelHeight: Float,
                    maxLineThickness: Float
                ): Float =0f
            }
        }
    }

    val fontSize = if (booleanResource(R.bool.isTablet)) 14.sp else 10.sp
    val label = rememberAxisLabelComponent(
        style = TextStyle(
            color = RuuviStationTheme.colors.chartLabel,
            fontSize = fontSize
        )
    )

    val axisGuideLine = rememberAxisGuidelineComponent(
        fill = Fill(RuuviStationTheme.colors.chartGuideline),
        shape = RectangleShape,
        thickness = 0.3.dp
    )

    val axisLine = rememberAxisLineComponent(
        fill = Fill(RuuviStationTheme.colors.chartAxisLine),
        thickness = 0.8.dp
    )

    val axisTick = rememberAxisTickComponent(
        fill = Fill(RuuviStationTheme.colors.chartGuideline),
        shape = RectangleShape,
        thickness = 0.3.dp
    )

    CartesianChartHost(
        chart =
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(lineStyles),
                    rangeProvider = CartesianLayerRangeProvider.fixed(
                        minY = minY - 1f,
                        maxY = maxY + 1f
                    ),
                ),
                startAxis = VerticalAxis.rememberStart(
                    line = axisLine,
                    label = label,
                    itemPlacer = customYAxisItemPlacer ?: rememberItemPlacerVertical(),
                    guideline = axisGuideLine,
                    tick = axisTick
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    line = axisLine,
                    label = label,
                    valueFormatter = rememberDateFormatter(),
                    itemPlacer = rememberItemPlacerHorizontal(),
                    guideline = axisGuideLine,
                    tick = axisTick
                ),
            ),
        modelProducer = modelProducer,
        zoomState = VicoZoomState(zoomEnabled = false, Zoom.Content, Zoom.Content, Zoom.Content),
        modifier = modifier,
    )
}