package com.ruuvi.station.settings.ui

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
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


    private var sensorManager: SensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        openFragment(viewModel.resourceId)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.let {
            it.registerListener(sensorListener, it.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val sensorListener: SensorEventListener = object : SensorEventListener {
        private var acceleration = 10f
        private var currentAcceleration = SensorManager.GRAVITY_EARTH
        private var lastAcceleration = SensorManager.GRAVITY_EARTH

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            lastAcceleration = currentAcceleration
            currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
            val delta: Float = currentAcceleration - lastAcceleration
            acceleration = acceleration * 0.9f + delta
            if (acceleration > 15) {
                Toast.makeText(applicationContext, "Shake event detected", Toast.LENGTH_SHORT).show()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
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
                R.string.gateway_url -> AppSettingsGatewayFragment.newInstance()
                R.string.settings_pressure_unit -> AppSettingsPressureUnitFragment.newInstance()
                R.string.settings_temperature_unit -> AppSettingsTemperatureUnitFragment.newInstance()
                R.string.settings_humidity_unit -> AppSettingsHumidityFragment.newInstance()
                R.string.settings_language -> AppSettingsLocaleFragment.newInstance()
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
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
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