package com.ruuvi.station.feature

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
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
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class GraphActivity : AppCompatActivity() {
    companion object {
        val TAGID = "TAG_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)


        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        val tagId = intent.getStringExtra(TAGID)
        val tag = RuuviTag.get(tagId)
        if (tag == null) finish()

        supportActionBar!!.title = tag.dispayName.toUpperCase()

        val readings = TagSensorReading.getForTag(tagId)

        val tempData: MutableList<Entry> = ArrayList()
        val humidData: MutableList<Entry> = ArrayList()
        val pressureData: MutableList<Entry> = ArrayList()

        readings.map { reading ->
            tempData.add(Entry(reading.createdAt.time.toFloat(), reading.temperature.toFloat()))
            humidData.add(Entry(reading.createdAt.time.toFloat(), reading.humidity.toFloat()))
            pressureData.add(Entry(reading.createdAt.time.toFloat(), reading.pressure.toFloat()))
        }

        addDataToChart(tempData, tempChart, "Temperature")
        addDataToChart(humidData, humidChart, "Humidity")
        addDataToChart(pressureData, pressureChart, "Pressure")
    }

    fun addDataToChart(data: MutableList<Entry>, chart: LineChart, label: String) {
        val set = LineDataSet(data, label)
        set.setDrawValues(false)
        set.setDrawFilled(true)
        chart.xAxis.textColor = Color.WHITE
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.getAxis(YAxis.AxisDependency.LEFT).textColor = Color.WHITE
        chart.getAxis(YAxis.AxisDependency.RIGHT).setDrawLabels(false)
        chart.description.text = label
        chart.description.textColor = Color.WHITE
        chart.legend.isEnabled = false
        chart.data = LineData(set)

        chart.xAxis.valueFormatter = object : IAxisValueFormatter {
            private val mFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            override fun getFormattedValue(value: Float, axis: AxisBase): String {
                return mFormat.format(Date(value.toLong()))
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
