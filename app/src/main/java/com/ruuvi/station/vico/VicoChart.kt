package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.VicoZoomState
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
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
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.vico.model.ChartData

@Composable
fun VicoChartNoInteraction(
    chartHistory: ChartData,
    modifier: Modifier = Modifier
) {
    val minY = chartHistory.values.min()
    val maxY = chartHistory.values.max()

    val modelProducer = remember { CartesianChartModelProducer() }

    LaunchedEffect(Unit) {
        modelProducer.runTransaction {
            lineSeries { series(chartHistory.timestamps, chartHistory.values) }
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
                startAxis = VerticalAxis.rememberStart(
                    itemPlacer = rememberItemPlacerVertical(),
                    guideline = rememberAxisGuidelineComponent(shape = Shape.Rectangle, thickness = 0.5.dp)
                ),
                bottomAxis = HorizontalAxis.rememberBottom(
                    valueFormatter = rememberDateFormatter(),
                    itemPlacer = rememberItemPlacerHorizontal(),
                    guideline = rememberAxisGuidelineComponent(shape = Shape.Rectangle, thickness = 0.5.dp)
                ),
            ),
        modelProducer = modelProducer,
        zoomState = VicoZoomState(zoomEnabled = false, Zoom.Content, Zoom.Content, Zoom.Content),
        modifier = modifier,
    )
}