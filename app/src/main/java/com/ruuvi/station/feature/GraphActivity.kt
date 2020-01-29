package com.ruuvi.station.feature

import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.Toast
import com.ruuvi.station.R
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.GraphView
import com.ruuvi.station.util.Preferences
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.activity_graph.background_view
import kotlinx.android.synthetic.main.activity_graph.toolbar_title

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
        val tag = RuuviTagRepository.get(tagId)
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
        toolbar_title.text = tag.displayName?.toUpperCase()

        val handler = Handler()
        handler.post(object: Runnable {
            override fun run() {
                tag.id?.let {
                    GraphView(applicationContext).drawChart(it, findViewById(android.R.id.content))
                handler.postDelayed(this, 30000)
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }
}
