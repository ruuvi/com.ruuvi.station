package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisTickComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.dashed
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.vico.model.ChartData
import com.ruuvi.station.vico.model.SegmentType

@Composable
fun VicoChartNoInteraction(
    chartHistory: ChartData,
    modifier: Modifier = Modifier
) {
    val minY = chartHistory.minValue
    val maxY = chartHistory.maxValue
    val context = LocalContext.current

    val modelProducer = remember { CartesianChartModelProducer() }

    var lineStyles by remember { mutableStateOf<List<LineCartesianLayer.Line>>(emptyList()) }

    val solidLine =
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(RuuviStationTheme.colors.chartLine)),
            stroke = LineCartesianLayer.LineStroke.continuous(thickness = 1.dp)
        )

    val pointComponent = rememberShapeComponent(
        fill = fill(RuuviStationTheme.colors.chartLine),
        shape = CorneredShape.Pill,
    )
    val pointsOnlyLine = LineCartesianLayer.rememberLine(
            stroke = LineCartesianLayer.LineStroke.continuous(thickness = 0.dp),
            pointProvider = LineCartesianLayer.PointProvider.single(
                point = LineCartesianLayer.point(component = pointComponent, size = 1.dp)
            )
        )

    val dottedLine =
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(
                fill(RuuviStationTheme.colors.chartLine)
            ),
            stroke = LineCartesianLayer.LineStroke.dashed(
                thickness = 1.dp,
                dashLength = 0.8.dp,
                gapLength = 1.8.dp
            )
        )

    LaunchedEffect(chartHistory) {
        if (chartHistory.segments.isEmpty()) {
            lineStyles = emptyList()
            modelProducer.runTransaction { lineSeries {  } }
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
            lineSeries {
                chartHistory.segments.forEach { seg ->
                    series(seg.timestamps, seg.values)
                }
            }
        }
    }

    val fontSize = if (booleanResource(R.bool.isTablet)) 14.sp else 10.sp
    val label = rememberAxisLabelComponent(
        color = RuuviStationTheme.colors.chartLabel,
        typeface = ResourcesCompat.getFont(context, R.font.mulish_regular)!!,
        textSize = fontSize
    )

    val axisGuideLine = rememberAxisGuidelineComponent(
        fill = fill(RuuviStationTheme.colors.chartGuideline),
        shape = Shape.Rectangle,
        thickness = 0.3.dp
    )

    val axisLine = rememberAxisLineComponent(
        fill = fill(RuuviStationTheme.colors.chartAxisLine),
        thickness = 0.8.dp
    )

    val axisTick = rememberAxisTickComponent(
        fill = fill(RuuviStationTheme.colors.chartGuideline),
        shape = Shape.Rectangle,
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
                    itemPlacer = rememberItemPlacerVertical(),
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