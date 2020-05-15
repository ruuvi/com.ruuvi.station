package com.ruuvi.station.feature

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import com.ruuvi.station.R
import com.ruuvi.station.adapters.RuuviTagAdapter
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.util.Starter
import kotlinx.android.synthetic.main.activity_tag_details.main_drawerLayout
import kotlinx.android.synthetic.main.activity_tag_details.toolbar
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class DashboardActivity : AppCompatActivity() , KodeinAware {
    override val kodein by closestKodein()
    val bluetoothInteractor: BluetoothInteractor by instance()
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
        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()
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
                    tags.addAll(ArrayList(RuuviTagRepository.getAll(true)))
                    if (tags.size > 0) {
                        noTagsFound.visibility = View.GONE
                    } else
                        noTagsFound.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    handler.postDelayed(this, 500)
                }
            })

            if (starter.checkBluetooth()) {
                starter.startScanning()
            }
        } else {
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { parent, view, position, id ->
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
