package com.ruuvi.station.graph

import android.content.Context
import android.text.format.DateUtils
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.ruuvi.station.R
import com.ruuvi.station.graph.model.ChartSensorType
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
    var getFrom: () -> Long,
    var clearMarker: () -> Unit
): MarkerView(context, layoutResource) {

    private var tvContent: TextView = findViewById(R.id.tvContent)

    override fun getOffset(): MPPointF {
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat() -15)
    }

    init {
        setOnClickListener {
            clearMarker()
        }
    }

    override fun refreshContent(e: Entry, highlight: Highlight) {
        val date = Date(e.x.toLong() + getFrom.invoke())
        val timeText = DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ","")

        val dateText = DateUtils.formatDateTime(
            context,
            date.time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
        )

        val valueText = when (chartSensorType) {
            ChartSensorType.TEMPERATURE -> unitsConverter.getTemperatureRawString(e.y.toDouble(), Accuracy.Accuracy2)
            ChartSensorType.HUMIDITY -> unitsConverter.getHumidityRawString(e.y.toDouble(), accuracy = Accuracy.Accuracy2)
            ChartSensorType.PRESSURE -> unitsConverter.getPressureRawString(e.y.toDouble(), accuracy = Accuracy.Accuracy2)
            ChartSensorType.BATTERY -> unitsConverter.getVoltageEnvironmentValue(e.y.toDouble()).valueWithUnit
            ChartSensorType.RSSI -> unitsConverter.getSignalEnvironmentValue(e.y.toInt()).valueWithUnit
            ChartSensorType.ACCELERATION -> e.y.toDouble().toString()
            ChartSensorType.MOVEMENTS -> e.y.toInt().toString()
            ChartSensorType.CO2 -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy0, context.getString(R.string.unit_co2))
            ChartSensorType.VOC -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy0, context.getString(R.string.unit_voc))
            ChartSensorType.NOX -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy0, context.getString(R.string.unit_nox))
            ChartSensorType.PM25 -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy2, context.getString(R.string.unit_pm25))
            ChartSensorType.LUMINOSITY -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy0, context.getString(R.string.unit_luminosity))
            ChartSensorType.SOUND -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy2, context.getString(R.string.unit_sound))
            ChartSensorType.AQI -> unitsConverter.getValue(e.y.toDouble(), Accuracy.Accuracy2, context.getString(R.string.aqi))
        }

        tvContent.text ="$valueText\n$timeText\n$dateText"

        super.refreshContent(e, highlight)
    }
}