package com.ruuvi.station.graph

import android.graphics.Canvas
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
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
    override fun drawLabel(c: Canvas?, formattedLabel: String, x: Float, y: Float, anchor: MPPointF?, angleDegrees: Float) {
        val lines = formattedLabel.split("\n").toTypedArray()
        var yPoint = y
        for (line in lines) {
            Utils.drawXAxisValue(c, line, x, yPoint, mAxisLabelPaint, anchor, angleDegrees)
            yPoint += mAxisLabelPaint.textSize
        }
    }

    override fun computeAxisValues(min: Float, max: Float) {
        Timber.d("computeAxisValues ($min / $max) From = $from")

        val labelCount = mAxis.labelCount
        val range = abs(max - min).toDouble()

        if (labelCount == 0 || range <=0) {
            mAxis.mEntries = floatArrayOf()
            mAxis.mCenteredEntries = floatArrayOf()
            mAxis.mEntryCount = 0
        }

        val rawInterval = range / labelCount
        val interval = getClosestPredefinedInterval(rawInterval)

        val timeZoneOffset = if (interval > 10800000) TimeZone.getDefault().rawOffset else 0

        var firstPoint = ((from + min).toLong() / interval) * interval - from - timeZoneOffset - 2 * interval
        var lastPoint =  ((from + max).toLong() / interval) * interval - from - timeZoneOffset + 2 * interval

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
        mAxis.mEntries = FloatArray(numberOfPoints)

        for ((i, value) in ((firstPoint..lastPoint) step interval).withIndex()) {
            mAxis.mEntries[i] = value.toFloat()
            Timber.d("computeAxisValues Axis value: $value")
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