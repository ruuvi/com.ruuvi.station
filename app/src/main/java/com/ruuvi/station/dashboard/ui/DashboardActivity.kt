package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.network.data.NetworkSyncResultType
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.AppSettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.PermissionsHelper
import com.ruuvi.station.util.extensions.*
import kotlinx.android.synthetic.main.activity_dashboard.mainDrawerLayout
import kotlinx.android.synthetic.main.activity_dashboard.toolbar
import kotlinx.android.synthetic.main.content_dashboard.*
import kotlinx.android.synthetic.main.navigation_drawer.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber
import java.util.*
import kotlin.collections.MutableList
import kotlin.concurrent.scheduleAtFixedRate

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: DashboardActivityViewModel by viewModel()

    private lateinit var permissionsHelper: PermissionsHelper
    private var tags: MutableList<RuuviTag> = arrayListOf()
    private lateinit var adapter: RuuviTagAdapter
    private var getTagsTimer :Timer? = null
    private var signedIn = false

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

    override fun onResume() {
        super.onResume()
        getTagsTimer = Timer("DashboardActivityTimer", false)
        getTagsTimer?.scheduleAtFixedRate(0, 1000) {
            viewModel.updateTags()
            viewModel.updateNetworkStatus()
        }
    }

    override fun onPause() {
        super.onPause()
        getTagsTimer?.cancel()
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

        updateMenu(signedIn)

        navigationDrawerListView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            mainDrawerLayout.closeDrawers()
            when (position) {
                0 -> AddTagActivity.start(this)
                1 -> AppSettingsActivity.start(this)
                2 -> AboutActivity.start(this)
                3 -> SendFeedback()
                4 -> OpenUrl(WEB_URL)
                5 -> login()
            }
        }

        syncLayout.setOnClickListener {
            viewModel.networkDataSync()
        }

        viewModel.syncResultObserve.observe(this, Observer {syncResult ->
            val message = when (syncResult.type) {
                NetworkSyncResultType.NONE -> ""
                NetworkSyncResultType.SUCCESS -> getString(R.string.network_sync_result_success)
                NetworkSyncResultType.EXCEPTION -> getString(R.string.network_sync_result_exception, syncResult.errorMessage)
                NetworkSyncResultType.NOT_LOGGED -> getString(R.string.network_sync_result_not_logged)
            }
            if (message.isNotEmpty()) {
                Snackbar.make(mainDrawerLayout, message, Snackbar.LENGTH_SHORT).show()
                viewModel.syncResultShowed()
            }
        })

        viewModel.syncInProgressObserve.observe(this, Observer {
            if (it) {
                Timber.d("Sync in progress")
                syncNetworkButton.startAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate_indefinitely))
            } else {
                Timber.d("Sync not in progress")
                syncNetworkButton.clearAnimation()
            }
        })

        viewModel.userEmail.observe(this, Observer {
            var user = it
            if (user.isNullOrEmpty()) {
                user = getString(R.string.none)
                signedIn = false
            } else {
                signedIn = true
            }
            updateMenu(signedIn)
            loggedUserTextView.text = getString(R.string.network_user, user)
        })

        viewModel.syncStatus.observe(this, Observer {syncStatus->
            if (syncStatus.syncInProgress) {
                syncStatusTextView.text = getString(R.string.connected_reading_info)
            } else {
                val lastSyncString =
                    if (syncStatus.lastSync == Long.MIN_VALUE) {
                        getString(R.string.never)
                    } else {
                        Date(syncStatus.lastSync).describingTimeSince(this)
                    }
                syncStatusTextView.text = getString(R.string.network_synced, lastSyncString)
            }
        })
    }

    private fun login() {
        if (signedIn == false) {
            SignInActivity.start(this)
        } else {
            val builder = AlertDialog.Builder(this)
            with(builder)
            {
                setMessage(getString(R.string.sign_out_confirm))
                setPositiveButton(getString(R.string.yes)) { dialogInterface, i ->
                    viewModel.signOut()
                }
                setNegativeButton(getString(R.string.no)) { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                show()
            }
        }
    }

    private fun updateMenu(signed: Boolean) {
        navigationDrawerListView.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            getMainMenuItems(signed)
        )
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
