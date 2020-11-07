package com.ruuvi.station.network.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.android.synthetic.main.activity_tag_settings.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class ShareSensorActivity : AppCompatActivity() , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: ShareSensorViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_sensor)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        finish()
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG_ID = "TAG_ID"

        fun start(context: Context, tagId: String?) {
            val intent = Intent(context, ShareSensorActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            context.startActivity(intent)
        }
    }
}