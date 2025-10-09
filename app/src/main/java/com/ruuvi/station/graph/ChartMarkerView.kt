package com.ruuvi.station.graph

import android.content.Context
import android.text.format.DateUtils
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.ruuvi.station.R
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType
import java.text.DateFormat
import java.util.*

class ChartMarkerView @JvmOverloads
constructor(
    context: Context,
    layoutResource: Int,
    val unitType: UnitType,
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

        val accuracy = if (unitType == UnitType.AirQuality.AqiIndex) {
            Accuracy.Accuracy1
        } else {
            unitType.defaultAccuracy
        }
        val valueText = unitsConverter.getValue(e.y.toDouble(), accuracy, context.getString(unitType.unit))

        tvContent.text ="$valueText\n$timeText\n$dateText"

        super.refreshContent(e, highlight)
    }
}