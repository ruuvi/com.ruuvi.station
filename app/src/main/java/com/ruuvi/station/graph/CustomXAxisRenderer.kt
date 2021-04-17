package com.ruuvi.station.graph

import android.graphics.Canvas
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.renderer.XAxisRenderer
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.utils.Transformer
import com.github.mikephil.charting.utils.Utils
import com.github.mikephil.charting.utils.ViewPortHandler

class CustomXAxisRenderer(viewPortHandler: ViewPortHandler?, xAxis: XAxis?, trans: Transformer?) : XAxisRenderer(viewPortHandler, xAxis, trans) {
    override fun drawLabel(c: Canvas?, formattedLabel: String, x: Float, y: Float, anchor: MPPointF?, angleDegrees: Float) {
        val lines = formattedLabel.split("\n").toTypedArray()
        var yPoint = y
        for (line in lines) {
            Utils.drawXAxisValue(c, line, x, yPoint, mAxisLabelPaint, anchor, angleDegrees)
            yPoint += mAxisLabelPaint.textSize
        }
    }
}