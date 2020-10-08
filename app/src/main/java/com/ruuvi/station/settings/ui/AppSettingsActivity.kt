package com.ruuvi.station.settings.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.activity_app_settings.toolbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

@ExperimentalCoroutinesApi
class AppSettingsActivity : AppCompatActivity(), AppSettingsDelegate, KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: AppSettingsViewModel by viewModel()

    private var showingFragmentTitle = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        openFragment(viewModel.resourceId)
    }

    override fun openFragment(resourceId: Int) {
        viewModel.resourceId = resourceId
        showingFragmentTitle = resourceId
        val transaction = supportFragmentManager.beginTransaction()
        val fragment: Fragment?
        if (resourceId == -1 || resourceId == R.string.title_activity_app_settings) {
            fragment = AppSettingsListFragment()
            showingFragmentTitle = R.string.title_activity_app_settings
            if (resourceId == R.string.title_activity_app_settings) {
                transaction.setCustomAnimations(R.anim.enter_left, R.anim.exit_right)
            }
        } else {
            transaction.setCustomAnimations(R.anim.enter_right, R.anim.exit_left)
            fragment = when (resourceId) {
                R.string.pref_bgscan -> AppSettingsBackgroundScanFragment.newInstance()
                R.string.preferences_graph_settings -> AppSettingsGraphFragment.newInstance()
                R.string.gateway_url -> AppSettingsGatewayFragment.newInstance()
                R.string.pressure_unit -> AppSettingsPressureUnitFragment.newInstance()
                R.string.temperature_unit -> AppSettingsTemperatureUnitFragment.newInstance()
                R.string.humidity_unit -> AppSettingsHumidityFragment.newInstance()
                else -> throw IllegalArgumentException()
            }
        }
        transaction.replace(R.id.settings_frame, fragment)
            .commit()
        title = getString(showingFragmentTitle)
    }

    override fun onBackPressed() {
        if (showingFragmentTitle != R.string.title_activity_app_settings) {
            openFragment(R.string.title_activity_app_settings)
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }

    companion object {
        fun start(context: Context) {
            val settingsIntent = Intent(context, AppSettingsActivity::class.java)
            context.startActivity(settingsIntent)
        }
    }
}

interface AppSettingsDelegate {
    fun openFragment(resourceId: Int)
}