package com.ruuvi.station.graph

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.ruuvi.station.R
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import java.text.DateFormat
import java.util.*

class ChartMarkerView @JvmOverloads
    constructor(
    context: Context,
    layoutResource: Int,
    val chartSensorType: ChartSensorType,
    val unitsConverter: UnitsConverter,
    val getFrom: () -> Long
): MarkerView(context, layoutResource) {

    private var tvContent: TextView = findViewById(R.id.tvContent)

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat() -15)
    }

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val date = Date(e.x.toLong() + getFrom.invoke())
        val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ","")

        val valueText = when (chartSensorType) {
            ChartSensorType.TEMPERATURE -> unitsConverter.getTemperatureRawString(e.y.toDouble(), Accuracy.Accuracy2)
            ChartSensorType.HUMIDITY -> unitsConverter.getHumidityRawString(e.y.toDouble(), Accuracy.Accuracy2)
            ChartSensorType.PRESSURE -> unitsConverter.getPressureRawString(e.y.toDouble(), Accuracy.Accuracy2)
        }

        tvContent.text ="$valueText\n$timeText"

        super.refreshContent(e, highlight)
    }
}

enum class ChartSensorType {
    TEMPERATURE,
    HUMIDITY,
    PRESSURE
}