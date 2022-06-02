package com.ruuvi.station.settings.ui

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.databinding.ActivityAppSettingsBinding
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.ShakeEventListener
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance

class AppSettingsActivity : AppCompatActivity(R.layout.activity_app_settings), AppSettingsDelegate, KodeinAware {

    override val kodein by closestKodein()

    private val viewModel: AppSettingsViewModel by viewModel()

    private val preferencesRepository: PreferencesRepository by instance()

    private var showingFragmentTitle = -1

    private var sensorManager: SensorManager? = null
    private val sensorListener = ShakeEventListener {
        if (!preferencesRepository.isExperimentalFeaturesEnabled()) {
            preferencesRepository.setIsExperimentalFeaturesEnabled(true)
            //Toast.makeText(this, "Looks like you want to unlock experimental settings ;)", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var binding: ActivityAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        openFragment(viewModel.resourceId)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onResume() {
        super.onResume()
        sensorManager?.let {
            it.registerListener(sensorListener, it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(sensorListener)
    }

    override fun openFragment(resourceId: Int) {
        viewModel.resourceId = resourceId
        showingFragmentTitle = resourceId
        val transaction = supportFragmentManager.beginTransaction()
        val fragment: Fragment?
        if (resourceId == -1 || resourceId == R.string.menu_app_settings) {
            fragment = AppSettingsListFragment()
            showingFragmentTitle = R.string.menu_app_settings
            if (resourceId == R.string.menu_app_settings) {
                transaction.setCustomAnimations(R.anim.enter_left, R.anim.exit_right)
            }
        } else {
            transaction.setCustomAnimations(R.anim.enter_right, R.anim.exit_left)
            fragment = when (resourceId) {
                R.string.settings_background_scan -> AppSettingsBackgroundScanFragment.newInstance()
                R.string.settings_chart -> AppSettingsGraphFragment.newInstance()
                R.string.settings_data_forwarding -> AppSettingsDataForwardingFragment.newInstance()
                R.string.settings_pressure_unit -> AppSettingsPressureUnitFragment.newInstance()
                R.string.settings_temperature_unit -> AppSettingsTemperatureUnitFragment.newInstance()
                R.string.settings_humidity_unit -> AppSettingsHumidityFragment.newInstance()
                R.string.settings_experimental -> AppSettingsExperimentalFragment.newInstance()
                else -> throw IllegalArgumentException()
            }
        }
        transaction.replace(R.id.settings_frame, fragment)
            .commit()
        title = getString(showingFragmentTitle)
    }

    override fun onBackPressed() {
        if (showingFragmentTitle != R.string.menu_app_settings) {
            openFragment(R.string.menu_app_settings)
        } else {
            closeActivity()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return true
    }

    fun closeActivity() {
        if (viewModel.shouldRestartApp()) {
            StartupActivity.start(this, false)
        }
        finish()
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