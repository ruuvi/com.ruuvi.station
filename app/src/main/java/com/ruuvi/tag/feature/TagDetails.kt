package com.ruuvi.tag.feature

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.ruuvi.tag.R
import com.ruuvi.tag.model.RuuviTag
import com.ruuvi.tag.scanning.RuuviTagListener
import com.ruuvi.tag.scanning.RuuviTagScanner
import com.ruuvi.tag.util.Utils

import kotlinx.android.synthetic.main.activity_tag_details.*

class TagDetails : AppCompatActivity(), RuuviTagListener {
    var tagId: String = ""
    var tag: RuuviTag? = null
    var scanner: RuuviTagScanner? = null
    var tempText: TextView? = null
    var humidityText: TextView? = null
    var pressureText: TextView? = null
    var signalText: TextView? = null
    var updatedText: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_details)
        setSupportActionBar(toolbar)

        tempText = findViewById(R.id.tag_temp)
        humidityText = findViewById(R.id.tag_humidity)
        pressureText = findViewById(R.id.tag_pressure)
        signalText = findViewById(R.id.tag_signal)
        updatedText = findViewById(R.id.tag_updated)

        tagId = intent.getStringExtra("id");
        tag = RuuviTag.get(tagId)

        updateUI()

        scanner = RuuviTagScanner(this, this)
    }

    override fun onResume() {
        super.onResume()
        scanner?.start()
    }

    override fun onPause() {
        super.onPause()
        scanner?.stop()
    }

    override fun tagFound(tag: RuuviTag) {
        if (tag.id == tagId) {
            this.tag?.updateDataFrom(tag);
            this.tag?.update()
            updateUI()
        }
    }

    fun updateUI() {
        tag?.let {
            tempText?.text = String.format(this.getString(R.string.temperature_reading), tag?.temperature)
            humidityText?.text = String.format(this.getString(R.string.humidity_reading), tag?.humidity)
            pressureText?.text = String.format(this.getString(R.string.pressure_reading), tag?.pressure)
            signalText?.text = String.format(this.getString(R.string.signal_reading), tag?.rssi)
            var updatedAt = this.getResources().getString(R.string.updated) + " " + Utils.strDescribingTimeSince(tag?.updateAt);
            updatedText?.text = updatedAt
        }
    }
}
