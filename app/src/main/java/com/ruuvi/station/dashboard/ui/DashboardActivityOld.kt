package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AdapterView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.app.permissions.NotificationPermissionInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.permissions.PermissionsInteractor
import com.ruuvi.station.databinding.ActivityDashboardBinding
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.ui.MyAccountActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.SettingsActivity
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.disableNavigationViewScrollbars
import com.ruuvi.station.util.extensions.openUrl
import com.ruuvi.station.util.extensions.sendFeedback
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.flow.collectLatest
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance


class DashboardActivityOld : AppCompatActivity(R.layout.activity_dashboard), KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: DashboardActivityViewModel by viewModel()

    private lateinit var binding: ActivityDashboardBinding

    private val unitsConverter: UnitsConverter by instance()
    private val movementConverter: MovementConverter by instance()

    private var tags: MutableList<RuuviTag> = arrayListOf()
    private val adapter: RuuviTagAdapter by lazy {
        RuuviTagAdapter(this@DashboardActivityOld, unitsConverter, movementConverter, tags)
    }
    private var signedIn = false
    private val preferencesRepository: PreferencesRepository by instance()
    private val permissionsInteractor: PermissionsInteractor = PermissionsInteractor(this)
    private val notificationPermissionInteractor = NotificationPermissionInteractor(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.title = null
        supportActionBar?.setIcon(R.drawable.logo_2021)

        setupViewModel()
        setupDrawer()
        setupListView()

        requestPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PermissionsInteractor.REQUEST_CODE_BLUETOOTH || requestCode == PermissionsInteractor.REQUEST_CODE_LOCATION) {
            requestPermission()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionsInteractor.REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestPermission()
                } else {
                    permissionsInteractor.showPermissionSnackbar()
                    askForNotificationPermission()
                }
            }
        }
    }

    private fun setupViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.tagsFlow.collectLatest {
                tags.clear()
                tags.addAll(it)
                binding.content.noTagsTextView.isVisible = tags.isEmpty()
                adapter.notifyDataSetChanged()
            }
        }
        observeSyncStatus()
    }

    private fun observeSyncStatus() {
        lifecycleScope.launchWhenStarted {
            viewModel.syncEvents.collect {
                if (it is NetworkSyncEvent.Unauthorised) {
                    viewModel.signOut()
                    signIn()
                }
            }
        }
    }

    private fun setupListView() {
        binding.content.dashboardListView.adapter = adapter
        binding.content.dashboardListView.onItemClickListener = tagClick
    }

    private fun setupDrawer() {
        val drawerToggle =
            ActionBarDrawerToggle(
                this, binding.mainDrawerLayout, binding.toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close
            )

        binding.mainDrawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        disableNavigationViewScrollbars(binding.navigationContent.navigationView)

        drawerToggle.syncState()

        updateMenu(signedIn)

        binding.navigationContent.navigationView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.addNewSensorMenuItem -> AddTagActivity.start(this)
                R.id.appSettingsMenuItem -> SettingsActivity.start(this)
                R.id.aboutMenuItem -> AboutActivity.start(this)
                R.id.sendFeedbackMenuItem -> sendFeedback()
                R.id.whatTomeasureMenuItem -> openUrl(getString(R.string.what_to_measure_link))
                R.id.getMoreSensorsMenuItem -> openUrl(getString(R.string.buy_sensors_link))
                R.id.getGatewayMenuItem -> openUrl(getString(R.string.buy_gateway_link))
                R.id.loginMenuItem ->  {
                    if (signedIn) {
                        MyAccountActivity.start(this)
                    } else {
                        signIn()
                    }
                }
            }
            binding.mainDrawerLayout.closeDrawer(GravityCompat.START)
            return@setNavigationItemSelectedListener true
        }

        viewModel.userEmail.observe(this) { user ->
            signedIn = !user.isNullOrEmpty()
            updateMenu(signedIn)
        }
    }

    private fun signIn() {
        SignInActivity.start(this)
    }

    private fun updateMenu(signed: Boolean) {
        val loginMenuItem = binding.navigationContent.navigationView.menu.findItem(R.id.loginMenuItem)
        loginMenuItem?.let {
            it.title = if (signed) {
                getString(R.string.my_ruuvi_account)
            } else {
                getString(R.string.sign_in)
            }
        }
    }

    private val tagClick = AdapterView.OnItemClickListener { _, view, _, _ ->
        val tag = view.tag as RuuviTag
        TagDetailsActivity.start(this, tag.id)
    }

    private fun requestPermission() {
        if (permissionsInteractor.arePermissionsGranted()) {
            askForNotificationPermission()
        } else {
            permissionsInteractor.requestPermissions(
                needBackground = viewModel.shouldAskForBackgroundLocationPermission,
                askForBluetooth = !preferencesRepository.isCloudModeEnabled() || !preferencesRepository.signedIn()
            )
        }
    }

    private fun askForNotificationPermission() {
        if (viewModel.shouldAskNotificationPermission) {
            notificationPermissionInteractor.checkAndRequest()
        }
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DashboardActivity::class.java)
            context.startActivity(intent)
        }
    }
}
