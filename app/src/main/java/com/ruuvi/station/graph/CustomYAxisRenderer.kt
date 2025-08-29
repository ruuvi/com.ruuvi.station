package com.ruuvi.station.graph

import android.graphics.Canvas
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.renderer.YAxisRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.math.abs
import kotlin.math.round

class CustomYAxisRenderer(
    viewPortHandler: ViewPortHandler?,
    yAxis: YAxis?,
    trans: Transformer?
): YAxisRenderer(viewPortHandler, yAxis, trans) {

    override fun drawYLabels(
        c: Canvas,
        fixedPosition: Float,
        positions: FloatArray,
        offset: Float
    ) {
        val from = if (mYAxis.isDrawBottomYLabelEntryEnabled) 0 else 1
        val to =
            if (mYAxis.isDrawTopYLabelEntryEnabled) mYAxis.mEntryCount else mYAxis.mEntryCount - 1
        val xOffset = mYAxis.labelXOffset

        for (i in from until to) {
            val text = mYAxis.getFormattedLabel(i)
            val x = fixedPosition + xOffset
            val y = positions[i * 2 + 1] + offset
            if (y < mViewPortHandler.contentBottom()) {
                c.drawText(
                    text,
                    x,
                    y,
                    mAxisLabelPaint
                )
            }
        }
    }

    override fun computeAxisValues(min: Float, max: Float) {
        if (min == Float.MAX_VALUE || max == Float.MAX_VALUE) {
            super.computeAxisValues(min, max)
            return
        }

        val labelCount = mYAxis.labelCount
        val range = abs(max - min).toDouble()

        if (labelCount == 0 || range <=0) {
            mYAxis.mEntries = doubleArrayOf()
            mYAxis.mCenteredEntries = floatArrayOf()
            mYAxis.mEntryCount = 0
        }

        val interval = getClosestPredefinedInterval(range, labelCount)

        var firstPoint = round(min / interval) * interval
        var lastPoint = round(max / interval) * interval

        if (range < interval) {
            firstPoint = min.toDouble()
            lastPoint = firstPoint
        }

        val numberOfPoints = if (interval != 0.0) {
            round(abs(lastPoint - firstPoint) / interval).toInt() + 1
        } else {
            1
        }

        mYAxis.mEntryCount = numberOfPoints
        mYAxis.mEntries = DoubleArray(numberOfPoints)

        var pointValue = firstPoint
        for (index in (0 until numberOfPoints)) {
            mYAxis.mEntries[index] = pointValue
            pointValue += interval
        }
    }

    private fun getClosestPredefinedInterval (range: Double, labelCount: Int): Double {
        return intervals.sortedBy { abs(range/it - labelCount) }.first()
    }

    companion object {
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