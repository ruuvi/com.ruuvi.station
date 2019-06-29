package com.ruuvi.station.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.FileProvider
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.ruuvi.station.R
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class GraphView (val context: Context) {
    private var from: Long = 0
    private var to: Long = 0

    fun drawChart(tagId: String, view: View) {
        val readings = TagSensorReading.getForTag(tagId)

        val tempData: MutableList<Entry> = ArrayList()
        val humidData: MutableList<Entry> = ArrayList()
        val pressureData: MutableList<Entry> = ArrayList()

        val tempUnit = RuuviTag.getTemperatureUnit(context)

        val cal = Calendar.getInstance()
        to = cal.time.time;
        cal.add(Calendar.HOUR, -24)
        from = cal.time.time

        if (readings.size > 0) {
            if (from < readings[0].createdAt.time)
                from = readings[0].createdAt.time
            val grouped = readings.groupBy {
                if (readings.size > 500)
                    it.createdAt.day.toString() + ":" + it.createdAt.hours.toString() + ":" + String.format("%02d", it.createdAt.minutes)[0]
                else if (readings.size > 100)
                    it.createdAt.day.toString() + ":" + it.createdAt.hours.toString() + ":" + it.createdAt.minutes.toString()
                else
                    it.createdAt.day.toString() + ":" + it.createdAt.hours.toString() + ":" + it.createdAt.minutes.toString() + ":" + it.createdAt.seconds.toString()
            }
            grouped.map {
                val reading = it.value[it.value.size - 1]
                val timestamp = (reading.createdAt.time - from).toFloat()
                if (tempUnit.equals("C")) tempData.add(Entry(timestamp, reading.temperature.toFloat()))
                else tempData.add(Entry(timestamp, Utils.celciusToFahrenheit(reading.temperature).toFloat()))
                humidData.add(Entry(timestamp, reading.humidity.toFloat()))
                pressureData.add(Entry(timestamp, reading.pressure.toFloat()))
            }
        } else {
            val timestamp = to.toFloat()
            tempData.add(Entry(timestamp, Utils.celciusToFahrenheit(0.0).toFloat()))
            humidData.add(Entry(timestamp, 0f))
            pressureData.add(Entry(timestamp, 0f))
        }

        addDataToChart(tempData, view.findViewById(R.id.tempChart), "Temperature")
        addDataToChart(humidData, view.findViewById(R.id.humidChart), "Humidity")
        addDataToChart(pressureData, view.findViewById(R.id.pressureChart), "Pressure")
    }

    fun addDataToChart(data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.highLightColor = context.resources.getColor(R.color.main)
        set.circleRadius = (2).toFloat()
        chart.xAxis.axisMaximum = (to - from).toFloat()
        chart.xAxis.axisMinimum = 0f
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
        chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
        chart.description.text = label
        chart.description.textColor = Color.WHITE
        chart.description.textSize = context.resources.getDimension(R.dimen.graph_description_size)
        chart.setNoDataTextColor(Color.WHITE)
        try {
            chart.description.typeface = ResourcesCompat.getFont(context, R.font.montserrat)
        } catch (e: Exception) { /* ¯\_(ツ)_/¯ */ }
        chart.legend.isEnabled = false
        chart.data = LineData(set)
        chart.data.isHighlightEnabled = false

        chart.xAxis.valueFormatter = object : IAxisValueFormatter {
            private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float, axis: AxisBase): String {
                return mFormat.format(Date(value.toLong() + from))
            }
        }

        chart.notifyDataSetChanged()
        chart.invalidate()
    }

}