package com.ruuvi.station.feature

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ruuvi.station.R

class GraphActivity : AppCompatActivity() {
    companion object {
        val TAGID = "TAG_ID"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)
    }
}
