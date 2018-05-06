package com.ruuvi.station.feature

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.ActionBarDrawerToggle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.ruuvi.station.R
import com.ruuvi.station.adapters.RuuviTagAdapter
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.util.Starter
import kotlinx.android.synthetic.main.activity_tag_details.*

class DashboardActivity : AppCompatActivity() {

    lateinit var handler: Handler
    lateinit var starter: Starter
    lateinit var tags: MutableList<RuuviTag>
    lateinit var adapter: RuuviTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo)

        setupDrawer()

        tags = RuuviTag.getAll(true)
        val noTagsFound = findViewById<TextView>(R.id.noTags_textView)

        val beaconListView = findViewById<ListView>(R.id.dashboard_listView)
        adapter = RuuviTagAdapter(applicationContext, tags)
        beaconListView.adapter = adapter

        beaconListView.onItemClickListener = tagClick

        handler = Handler()

        starter = Starter(this)
        starter.getThingsStarted()
    }

    fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
                this, main_drawerLayout, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        main_drawerLayout.addDrawerListener(drawerToggle)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setHomeButtonEnabled(true)
        }
        drawerToggle.syncState()

        val drawerListView = findViewById<ListView>(R.id.navigationDrawer_listView)

        drawerListView.adapter = ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                resources.getStringArray(R.array.navigation_items_card_view)
        )

        drawerListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            main_drawerLayout.closeDrawers()
            when (i) {
                1 -> {
                    val settingsIntent = Intent(this, AppSettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                2 -> {
                    val aboutIntent = Intent(this, AboutActivity::class.java)
                    startActivity(aboutIntent)
                }
                else -> {
                    val addIntent = Intent(this, AddTagActivity::class.java)
                    startActivity(addIntent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(object: Runnable {
            override fun run() {
                handler.postDelayed(this, 1000)
            }
        })

        if (starter.getNeededPermissions().isEmpty()) {
            val noTagsFound = findViewById<TextView>(R.id.noTags_textView)
            handler.post(object : Runnable {
                override fun run() {
                    tags.clear()
                    tags.addAll(RuuviTag.getAll(true))
                    if (tags.size > 0) {
                        noTagsFound.visibility = View.GONE
                    } else
                        noTagsFound.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    handler.postDelayed(this, 1000)
                }
            })

            if (starter.checkBluetooth()) {
                starter.startScanning()
            }
        } else {
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { parent, view, position, id ->
        val tag = view.tag as RuuviTag
        val intent = Intent(applicationContext, TagDetails::class.java)
        intent.putExtra("id", tag.id)
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }
}
