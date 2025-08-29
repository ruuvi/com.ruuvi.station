package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasuringContext
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.layer.CartesianLayerDimensions
import timber.log.Timber
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun rememberItemPlacerHorizontal() =
    remember {
        object : HorizontalAxis.ItemPlacer {
            override fun getEndLayerMargin(
                context: CartesianMeasuringContext,
                layerDimensions: CartesianLayerDimensions,
                tickThickness: Float,
                maxLabelWidth: Float
            ): Float = 0f

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

                val localOffset =
                    if (interval > 3600000) TimeZone.getDefault().getOffset(start) else 0

                var tick = start - localOffset
                while (tick <= xMax) {
                    if (tick >= xMin) {
                        positions.add(tick.toDouble())
                    }
                    tick += interval
                }
                return positions
            }


            override fun getStartLayerMargin(
                context: CartesianMeasuringContext,
                layerDimensions: CartesianLayerDimensions,
                tickThickness: Float,
                maxLabelWidth: Float
            ): Float = maxLabelWidth / 2

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
    }