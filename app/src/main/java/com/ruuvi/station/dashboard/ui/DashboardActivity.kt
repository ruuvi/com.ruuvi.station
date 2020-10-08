package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.PermissionsHelper
import com.ruuvi.station.util.extensions.OpenUrl
import com.ruuvi.station.util.extensions.SendFeedback
import kotlinx.android.synthetic.main.activity_tag_details.*
import kotlinx.android.synthetic.main.content_dashboard.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import kotlin.collections.MutableList

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: DashboardActivityViewModel by viewModel()

    private lateinit var permissionsHelper: PermissionsHelper
    private var tags: MutableList<RuuviTag> = arrayListOf()
    private lateinit var adapter: RuuviTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        setSupportActionBar(toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo)

        setupViewModel()
        setupDrawer()
        setupListView()

        permissionsHelper = PermissionsHelper(this)
        permissionsHelper.requestPermissions()
    }

    private fun setupViewModel() {
        viewModel.observeTags.observe( this, Observer {
            tags.clear()
            tags.addAll(it)
            noTagsTextView.isVisible = tags.isEmpty()
            adapter.notifyDataSetChanged()
        })
    }

    private fun setupListView() {
        adapter = RuuviTagAdapter(this@DashboardActivity, tags, viewModel.converter)
        dashboardListView.adapter = adapter
        dashboardListView.onItemClickListener = tagClick
    }

    private fun setupDrawer() {
        val drawerToggle =
            ActionBarDrawerToggle(
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
                0 -> AddTagActivity.start(this)
                1 -> AppSettingsActivity.start(this)
                2 -> AboutActivity.start(this)
                3 -> SendFeedback()
                4 -> OpenUrl(WEB_URL)
                5 -> SignInActivity.start(this)
            }
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { _, view, _, _ ->
        val tag = view.tag as RuuviTag
        TagDetailsActivity.start(this, tag.id)
    }

    companion object {
        private const val WEB_URL = "https://ruuvi.com"

        fun start(context: Context) {
            val intent = Intent(context, DashboardActivity::class.java)
            context.startActivity(intent)
        }
    }
}
