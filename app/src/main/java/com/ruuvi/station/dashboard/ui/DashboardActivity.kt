package com.ruuvi.station.dashboard.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.adapters.RuuviTagAdapter
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.PermissionsHelper
import kotlinx.android.synthetic.main.activity_tag_details.mainDrawerLayout
import kotlinx.android.synthetic.main.activity_tag_details.toolbar
import kotlinx.android.synthetic.main.content_dashboard.dashboardListView
import kotlinx.android.synthetic.main.content_dashboard.noTagsTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: DashboardActivityViewModel by viewModel()

    private lateinit var permissionsHelper: PermissionsHelper
    private lateinit var tags: MutableList<RuuviTag>
    private lateinit var adapter: RuuviTagAdapter

    private val timer = Timer("DashboardActivityTimer", false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo)

        setupDrawer()
        setupListView()

        dashboardListView.onItemClickListener = tagClick

        permissionsHelper = PermissionsHelper(this)
        permissionsHelper.requestPermissions()
    }

    private fun setupListView() {
        lifecycleScope.launchWhenCreated {
            viewModel.tagsFlow.value.let {
                tags = ArrayList(it)
                adapter = RuuviTagAdapter(applicationContext, tags)
                dashboardListView.adapter = adapter
            }
        }

        dashboardListView.onItemClickListener = tagClick
    }

    private fun setupDrawer() {
        val drawerToggle = ActionBarDrawerToggle(
            this, mainDrawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )

        mainDrawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        drawerToggle.syncState()

        val drawerListView = findViewById<ListView>(R.id.navigationDrawerListView)

        drawerListView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            resources.getStringArray(R.array.navigation_items_card_view)
        )

        drawerListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            mainDrawerLayout.closeDrawers()
            when (position) {
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

        CoroutineScope(Dispatchers.IO).launch {
            if (permissionsHelper.arePermissionsGranted()) {
                timer.scheduleAtFixedRate(1000, 500) {
                    lifecycleScope.launchWhenResumed {
                        viewModel.tagsFlow.collect {
                            tags.clear()
                            tags.addAll(it)
                            noTagsTextView.isVisible = tags.isEmpty()
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { _, view, _, _ ->
        val tag = view.tag as RuuviTag
        val intent = Intent(applicationContext, TagDetailsActivity::class.java)
        intent.putExtra("id", tag.id)
        startActivity(intent)
    }
}
