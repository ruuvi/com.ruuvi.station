package com.ruuvi.station.graph

import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler
import timber.log.Timber
import java.util.*
import kotlin.math.abs

class CustomXAxisRenderer(
    private val from: Long,
    viewPortHandler: ViewPortHandler?,
    xAxis: XAxis?,
    trans: Transformer?
) : XAxisRenderer(viewPortHandler, xAxis, trans) {

    override fun computeAxisValues(min: Float, max: Float) {
        Timber.d("computeAxisValues ($min / $max) From = $from")

        val labelCount = mAxis.labelCount
        val range = abs(max - min).toDouble()
        Timber.d("computeAxisValues range $range")

        if (labelCount == 0 || range <=0) {
            mAxis.mEntries = doubleArrayOf()
            mAxis.mCenteredEntries = floatArrayOf()
            mAxis.mEntryCount = 0
        }

        val rawInterval = range / labelCount
        Timber.d("computeAxisValues rawInterval $rawInterval")

        val interval = getClosestPredefinedInterval(rawInterval)
        Timber.d("computeAxisValues interval $interval")

        var firstPoint = ((from + min).toLong() / interval) * interval - from  - 2 * interval
        var lastPoint =  ((from + max).toLong() / interval) * interval - from  + 2 * interval
        Timber.d("computeAxisValues firstPoint $firstPoint lastPoint $lastPoint")

        if (range < interval) {
            firstPoint = min.toLong()
            lastPoint = firstPoint
        }

        var numberOfPoints = 0
        if (lastPoint != firstPoint) {
            for (f in firstPoint..lastPoint step interval) {
                numberOfPoints++
            }
        } else {
            numberOfPoints = 1
        }

        mAxis.mEntryCount = numberOfPoints
        mAxis.mEntries = DoubleArray(numberOfPoints)
        Timber.d("computeAxisValues labelCount = $labelCount numberOfPoints = $numberOfPoints")

        for ((i, value) in ((firstPoint..lastPoint) step interval).withIndex()) {
            val date = Date(from + value)
            val localOffset = if (interval > 3600000) TimeZone.getDefault().getOffset(date.time) else 0

            mAxis.mEntries[i] = (value - localOffset).toDouble()
        }
    }

    private fun getClosestPredefinedInterval (rawInterval: Double): Long {
        return intervals.sortedBy { abs(it - rawInterval) }.first()
    }

    companion object {
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