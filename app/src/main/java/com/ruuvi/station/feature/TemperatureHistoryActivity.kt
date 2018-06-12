package com.ruuvi.station.feature

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.ListView
import com.ruuvi.station.R
import com.ruuvi.station.adapters.TempHistoryAdapter
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
    var historyItemList = ArrayList<HistoryItem>()
    lateinit var adapter: TempHistoryAdapter

    val sdf = SimpleDateFormat("dd.MM.yyyy")

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


        val temperature = tag?.getTemperatureString(this)
        val unit = temperature.substring(temperature.length - 2, temperature.length)

        var cursor = TagSensorReading.getTempHistory(tag?.id)

        while (cursor.moveToNext()) {
            var index: Int

            index = cursor.getColumnIndexOrThrow("createdAt")
            val createdAt = cursor.getString(index)

            index = cursor.getColumnIndexOrThrow("min")
            val min = cursor.getString(index)

            index = cursor.getColumnIndexOrThrow("max")
            val max = cursor.getString(index)

            historyItemList.add(HistoryItem(createdAt, min, max, unit))
        }

        historyListView.adapter = TempHistoryAdapter(this, historyItemList.reversed())
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
