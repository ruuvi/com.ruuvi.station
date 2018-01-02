package com.ruuvi.station.feature

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.ruuvi.station.R
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import kotlinx.android.synthetic.main.activity_graph.*

class GraphActivity : AppCompatActivity() {
    companion object {
        val TAGID = "TAG_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        val tagId = intent.getStringExtra(TAGID)
        var tag = RuuviTag.get(tagId)
        if (tag == null) finish()

        var readings = TagSensorReading.getForTag(tagId)

        var tempData: MutableList<Entry> = ArrayList()

        readings.map { reading ->
            tempData.add(Entry(reading.createdAt.time.toFloat(), reading.temperature.toFloat()))
        }

        var sets: MutableList<LineDataSet> = ArrayList()
        sets.add(LineDataSet(tempData, "temp"))
        lineChart.data = LineData(sets.toList())
    }
}
