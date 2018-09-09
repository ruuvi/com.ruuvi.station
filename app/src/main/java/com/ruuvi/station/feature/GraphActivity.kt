package com.ruuvi.station.feature

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.preference.PreferenceManager
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.ruuvi.station.R
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import kotlinx.android.synthetic.main.activity_graph.*
import javax.xml.datatype.DatatypeConstants.HOURS
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.ruuvi.station.feature.main.MainActivity
import com.ruuvi.station.util.Utils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GraphActivity : AppCompatActivity() {
    companion object {
        val TAGID = "TAG_ID"
    }

    var firstReadingTime: Long = 0
    var tagId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        tagId = intent.getStringExtra(TAGID)
        val tag = RuuviTag.get(tagId)
        if (tag == null) {
            finish()
            return
        }

        Utils.getBackground(applicationContext, tag).let { bitmap ->
            background_view.setImageDrawable(BitmapDrawable(applicationContext.resources, bitmap))
        }

        val settings = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        val bgScanEnabled = settings.getBoolean("pref_bgscan", false)
        if (!bgScanEnabled) {
            Toast.makeText(applicationContext, resources.getText(R.string.bg_scan_for_graphs), Toast.LENGTH_LONG).show()

            if (settings.getBoolean("first_graph_visit", true)) {
                val simpleAlert = AlertDialog.Builder(this).create()
                simpleAlert.setTitle(resources.getText(R.string.bg_scan_for_graphs))
                simpleAlert.setMessage(resources.getText(R.string.enable_background_scanning_question))

                simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes), {
                    _, _ ->
                    settings.edit().putBoolean("pref_bgscan", true).commit()
                    MainActivity.setBackgroundScanning(true, application, settings)
                })
                simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no), {
                    _, _ ->
                    settings.edit().putBoolean("first_graph_visit", false).apply()
                })

                simpleAlert.show()
            }
        }

        supportActionBar!!.title = ""
        toolbar_title.text = tag.dispayName.toUpperCase()

        val handler = Handler()
        handler.post(object: Runnable {
            override fun run() {
                drawChart()
                handler.postDelayed(this, 30000)
            }
        })
    }

    fun drawChart() {
        val readings = TagSensorReading.getForTag(tagId)
        if (readings.size == 0) return

        val tempData: MutableList<Entry> = ArrayList()
        val humidData: MutableList<Entry> = ArrayList()
        val pressureData: MutableList<Entry> = ArrayList()

        firstReadingTime = readings[0].createdAt.time

        val tempUnit = RuuviTag.getTemperatureUnit(this)

        readings.map { reading ->
            val timestamp = (reading.createdAt.time - firstReadingTime).toFloat()
            if (tempUnit.equals("C")) tempData.add(Entry(timestamp, reading.temperature.toFloat()))
            else tempData.add(Entry(timestamp, Utils.celciusToFahrenheit(reading.temperature).toFloat()))
            humidData.add(Entry(timestamp, reading.humidity.toFloat()))
            pressureData.add(Entry(timestamp, reading.pressure.toFloat()))
        }

        addDataToChart(tempData, tempChart, "Temperature")
        addDataToChart(humidData, humidChart, "Humidity")
        addDataToChart(pressureData, pressureChart, "Pressure")
    }

    fun addDataToChart(data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawValues(false)
        set.setDrawFilled(true)
        set.highLightColor = resources.getColor(R.color.main)
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
        chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
        chart.description.text = label
        chart.description.textColor = Color.WHITE
        chart.description.textSize = applicationContext.resources.getDimension(R.dimen.graph_description_size)
        try {
            chart.description.typeface = ResourcesCompat.getFont(applicationContext, R.font.montserrat)
        } catch (e: Exception) { /* ¯\_(ツ)_/¯ */ }
        chart.legend.isEnabled = false
        chart.data = LineData(set)

        chart.xAxis.valueFormatter = object : IAxisValueFormatter {
            private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float, axis: AxisBase): String {
                return mFormat.format(Date(firstReadingTime + value.toLong()))
            }
        }

        chart.notifyDataSetChanged()
        chart.invalidate()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
