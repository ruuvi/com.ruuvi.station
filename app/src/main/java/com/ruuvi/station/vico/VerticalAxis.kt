package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartRanges
import com.patrykandpatrick.vico.core.common.Position
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

@Composable
fun rememberItemPlacerVertical(modifier: Modifier = Modifier) =
    remember {
        object : VerticalAxis.ItemPlacer {
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

                val start = floor(yRange.minY.toLong() / interval) * interval

                var tick = start
                while (tick <= yRange.maxY) {
                    if (tick >= yRange.minY) {
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
    }