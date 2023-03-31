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

        if (labelCount == 0 || range <=0) {
            mAxis.mEntries = doubleArrayOf()
            mAxis.mCenteredEntries = floatArrayOf()
            mAxis.mEntryCount = 0
        }

        val rawInterval = range / labelCount
        val interval = getClosestPredefinedInterval(rawInterval)

        val timeZoneOffset = if (interval > 3600000) TimeZone.getDefault().getOffset(Date().time) else 0
        Timber.d("computeAxisValues timeZoneOffset = $timeZoneOffset")

        var firstPoint = ((from + min).toLong() / interval) * interval - from  - 2 * interval
        var lastPoint =  ((from + max).toLong() / interval) * interval - from  + 2 * interval

        if (range < interval) {
            firstPoint = min.toLong()
            lastPoint = firstPoint
        }

        var numberOfPoints = 0
        if (interval != 0L && lastPoint != firstPoint) {
            for (f in firstPoint..lastPoint step interval) {
                numberOfPoints++
            }
        } else {
            numberOfPoints = 1
        }

        mAxis.mEntryCount = numberOfPoints
        mAxis.mEntries = DoubleArray(numberOfPoints)

        for ((i, value) in ((firstPoint..lastPoint) step interval).withIndex()) {
            val date = Date(from + value)
            val localOffset = if (interval > 3600000) TimeZone.getDefault().getOffset(date.time) else 0

            mAxis.mEntries[i] = (value - localOffset).toDouble()
            Timber.d("computeAxisValues Axis value: ${from + value}  offset = $localOffset for $date")
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
        )
    }
}