package com.ruuvi.station.graph

import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

class CustomXAxisRenderer(
    private val from: Long,
    viewPortHandler: ViewPortHandler?,
    xAxis: XAxis?,
    trans: Transformer?
) : XAxisRenderer(viewPortHandler, xAxis, trans) {

    override fun computeAxisValues(min: Float, max: Float) {
        val range = abs(max.toDouble() - min.toDouble())
        if (mAxis.labelCount == 0 || range <= 0 || !range.isFinite()) {
            mAxis.mEntries = doubleArrayOf()
            mAxis.mCenteredEntries = floatArrayOf()
            mAxis.mEntryCount = 0
            return
        }
        mAxisLabelPaint.typeface = mAxis.typeface
        mAxisLabelPaint.textSize = mAxis.textSize
        // Measure the current date format too: month/year labels need more room than HH:mm.
        val textWidth = listOf(min.toDouble(), (min.toDouble() + max) / 2, max.toDouble())
            .maxOf { mAxisLabelPaint.measureText(mAxis.valueFormatter.getFormattedValue(it, mAxis)) }
        val fittingLabels = (mViewPortHandler.contentWidth() / (textWidth + Utils.convertDpToPixel(16f)))
            .toInt().coerceAtLeast(1)
        val count = minOf(mAxis.labelCount, fittingLabels)
        val interval = intervals.firstOrNull { it >= range / count } ?: intervals.last()
        val first = floor((from + min.toDouble()) / interval).toLong() * interval - 2 * interval
        val last = ceil((from + max.toDouble()) / interval).toLong() * interval + 2 * interval
        val entries = (first..last step interval).map { timestamp ->
            // Calendar fields, rather than a fixed UTC offset, keep ticks at local
            // midnight across DST boundaries, including fractional-offset time zones.
            val aligned = if (interval >= 3_600_000L) {
                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = timestamp }
                Calendar.getInstance().apply {
                    clear()
                    set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH),
                        utc.get(Calendar.HOUR_OF_DAY), 0, 0)
                }.timeInMillis
            } else timestamp
            (aligned - from).toDouble()
        }.distinct().sorted()
        mAxis.mEntries = entries.toDoubleArray()
        mAxis.mEntryCount = entries.size
    }

    companion object {
        val intervals = longArrayOf(
            60_000, 120_000, 180_000, 300_000, 600_000, 900_000, 1_800_000,
            3_600_000, 7_200_000, 10_800_000, 21_600_000, 43_200_000,
            86_400_000, 172_800_000, 345_600_000, 691_200_000, 1_209_600_000,
            30 * 86_400_000L, 60 * 86_400_000L, 90 * 86_400_000L,
            180 * 86_400_000L, 365 * 86_400_000L, 730 * 86_400_000L, 1024 * 86_400_000L
        )
    }
}
