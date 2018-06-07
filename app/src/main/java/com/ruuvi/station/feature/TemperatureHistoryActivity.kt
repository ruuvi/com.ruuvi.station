package com.ruuvi.station.feature

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.widget.ListAdapter
import android.widget.ListView
import com.ruuvi.station.adapters.TempHistoryAdapter
import com.ruuvi.station.R
import com.ruuvi.station.model.HistoryItem
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.activity_temperature_history.*
import java.text.SimpleDateFormat
import java.util.*

class TemperatureHistoryActivity : AppCompatActivity() {
    companion object {
        val TAGID = "TAG_ID"
    }

    var tagId: String? = null
    var content = ArrayList<HistoryItem>()
    lateinit var adapter: TempHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_temperature_history)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)

        tagId = intent.getStringExtra(TemperatureHistoryActivity.TAGID)
        val tag = RuuviTag.get(tagId)
        if (tag == null) {
            finish()
            return
        }

        history_root_view.background = Utils.getDefaultBackground(tag.defaultBackground, this)

        supportActionBar!!.title = ""
        toolbar_title.text = tag.dispayName.toUpperCase()

        val historyListView = findViewById<ListView>(R.id.historyListView)

        var readingsSortedByDate = TagSensorReading.getForTag(tag?.id).sortedWith(compareBy({ it.createdAt }))

        var prevDate = readingsSortedByDate[0].createdAt

        val temperature = tag?.getTemperatureString(this)
        val unit = temperature.substring(temperature.length - 2, temperature.length)

        var dayValues = getOneDayValues(tag, prevDate)
        content.add(HistoryItem(dayValues[0].createdAt, dayValues[0].temperature, dayValues[dayValues.lastIndex].temperature, unit))

        for (entry in readingsSortedByDate) {
            if (prevDate.date != entry.createdAt.date) {
                var dayValues = getOneDayValues(tag, entry.createdAt)
                content.add(HistoryItem(dayValues[0].createdAt, dayValues[0].temperature, dayValues[dayValues.lastIndex].temperature, unit))
            }
            prevDate = entry.createdAt
        }
        adapter = TempHistoryAdapter(this, content.reversed())
        historyListView.adapter = adapter
    }

    fun getOneDayValues(tag: RuuviTag, date: Date): List<TagSensorReading> {
        val sdf = SimpleDateFormat("dd.MM.yyyy")
        val dateReadings = TagSensorReading.getForTag(tag?.id).filter { s -> sdf.format(s.createdAt) == sdf.format(date) }
        return dateReadings.sortedWith(compareBy({ it.temperature }))
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
