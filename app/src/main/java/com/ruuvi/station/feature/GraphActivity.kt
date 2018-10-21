package com.ruuvi.station.feature

import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.widget.Toast
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.ruuvi.station.R
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import kotlinx.android.synthetic.main.activity_graph.*
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.GraphView
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.Utils
import java.text.SimpleDateFormat
import java.util.*


class GraphActivity : AppCompatActivity() {
    companion object {
        val TAGID = "TAG_ID"
    }

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

        val prefs = Preferences(this)

        val bgScanEnabled = prefs.backgroundScanMode
        if (bgScanEnabled == BackgroundScanModes.DISABLED) {
            Toast.makeText(applicationContext, resources.getText(R.string.bg_scan_for_graphs), Toast.LENGTH_LONG).show()

            if (prefs.isFirstGraphVisit) {
                val simpleAlert = AlertDialog.Builder(this).create()
                simpleAlert.setTitle(resources.getText(R.string.bg_scan_for_graphs))
                simpleAlert.setMessage(resources.getText(R.string.enable_background_scanning_question))

                simpleAlert.setButton(AlertDialog.BUTTON_POSITIVE, resources.getText(R.string.yes)) { _, _ ->
                    prefs.backgroundScanMode = BackgroundScanModes.FOREGROUND
                }
                simpleAlert.setButton(AlertDialog.BUTTON_NEGATIVE, resources.getText(R.string.no)) { _, _ ->
                    prefs.isFirstGraphVisit = false
                }

                simpleAlert.show()
            }
        }

        supportActionBar!!.title = ""
        toolbar_title.text = tag.dispayName.toUpperCase()

        val handler = Handler()
        handler.post(object: Runnable {
            override fun run() {
                GraphView(applicationContext).drawChart(tag.id, findViewById(android.R.id.content))
                handler.postDelayed(this, 30000)
            }
        })
    }


    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
