package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.continuous
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.vico.model.ChartData

@Composable
fun VicoChartNoInteraction(
    chartHistory: ChartData,
    modifier: Modifier = Modifier
) {
    val minY = chartHistory.values.min()
    val maxY = chartHistory.values.max()
    val context = LocalContext.current

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries { series(chartHistory.timestamps, chartHistory.values) }
        }
    }

    val fontSize = if (booleanResource(R.bool.isTablet)) 14.sp else 10.sp
    val label = rememberAxisLabelComponent(
        color = Color.White,
        typeface = ResourcesCompat.getFont(context, R.font.mulish_regular)!!,
        textSize = fontSize
    )

    val axisGuideLine = rememberAxisGuidelineComponent(
        fill = fill(Color.White.copy(alpha = 0.3f)),
        shape = Shape.Rectangle,
        thickness = 0.3.dp
    )

    val axisLine = rememberAxisLineComponent(
        fill = fill(Color.White.copy(alpha = 0.3f)),
        thickness = 0.8.dp
    )

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
                startAxis = VerticalAxis.rememberStart(
                    line = axisLine,
                    label = label,
                    itemPlacer = rememberItemPlacerVertical(),
                    guideline = axisGuideLine
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    line = axisLine,
                    label = label,
                    valueFormatter = rememberDateFormatter(),
                    itemPlacer = rememberItemPlacerHorizontal(),
                    guideline = axisGuideLine
                ),
            ),
        modelProducer = modelProducer,
        zoomState = VicoZoomState(zoomEnabled = false, Zoom.Content, Zoom.Content, Zoom.Content),
        modifier = modifier,
    )
}