package com.ruuvi.station.dashboard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.core.view.isVisible
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.adapters.RuuviTagAdapter
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.feature.AboutActivity
import com.ruuvi.station.feature.AddTagActivity
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.Starter
import kotlinx.android.synthetic.main.activity_tag_details.main_drawerLayout
import kotlinx.android.synthetic.main.activity_tag_details.toolbar
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: DashboardActivityViewModel by viewModel()

    lateinit var handler: Handler
    lateinit var starter: Starter
    lateinit var tags: MutableList<RuuviTagEntity>
    lateinit var adapter: RuuviTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo)

        setupDrawer()

        tags = ArrayList(RuuviTagRepository.getAll(true))

        val beaconListView = findViewById<ListView>(R.id.dashboard_listView)
        adapter = RuuviTagAdapter(applicationContext, tags)
        beaconListView.adapter = adapter

        beaconListView.onItemClickListener = tagClick

        handler = Handler()

        starter = Starter(this)
        starter.getThingsStarted()
    }

    private fun setupDrawer() {
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
                0 -> {
                    val addIntent = Intent(this, AddTagActivity::class.java)
                    startActivity(addIntent)
                }
                1 -> {
                    val settingsIntent = Intent(this, AppSettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                2 -> {
                    val aboutIntent = Intent(this, AboutActivity::class.java)
                    startActivity(aboutIntent)
                }
                3 -> {
                    val url = "https://ruuvi.com"
                    val webIntent = Intent(Intent.ACTION_VIEW)
                    webIntent.data = Uri.parse(url)
                    startActivity(webIntent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startForegroundScanning()
        handler.post(object : Runnable {
            override fun run() {
                handler.postDelayed(this, 1000)
            }
        })

        if (starter.getNeededPermissions().isEmpty()) {
            val noTagsFound = findViewById<TextView>(R.id.noTags_textView)
            handler.post(object : Runnable {
                override fun run() {
                    tags.clear()
                    tags.addAll(ArrayList(RuuviTagRepository.getAll(true)))
                    noTagsFound.isVisible = tags.size <= 0
                    adapter.notifyDataSetChanged()
                    handler.postDelayed(this, 500)
                }
            })

            if (starter.checkBluetooth()) {
                starter.startScanning()
            }
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { _, view, _, _ ->
        val tag = view.tag as RuuviTagEntity
        val intent = Intent(applicationContext, TagDetailsActivity::class.java)
        intent.putExtra("id", tag.id)
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacksAndMessages(null)
    }
}
