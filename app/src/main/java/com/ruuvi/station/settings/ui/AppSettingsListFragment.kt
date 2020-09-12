package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.view.isVisible
import com.flexsentlabs.androidcommons.app.ui.setDebouncedOnClickListener
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.test.StressTestGenerator
import kotlinx.android.synthetic.main.fragment_app_settings_list.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class AppSettingsListFragment : Fragment(R.layout.fragment_app_settings_list), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsListViewModel by viewModel()

    private val repository: TagRepository by instance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        scanLayout.setDebouncedOnClickListener {
            (activity as? AppSettingsDelegate)?.openFragment(R.string.pref_bgscan)
        }

        scanIntervalLayout.setDebouncedOnClickListener {
            if (viewModel.getBackgroundScanMode() != BackgroundScanModes.DISABLED) {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.background_scan_interval)
            }
        }

        gatewaySettingsLayout.setDebouncedOnClickListener {
            (activity as? AppSettingsDelegate)?.openFragment(R.string.gateway_url)
        }

        graphSettingsLayout.setDebouncedOnClickListener {
            (activity as? AppSettingsDelegate)?.openFragment(R.string.preferences_graph_settings)
        }

        temperatureUnitLayout.setDebouncedOnClickListener {
            (activity as? AppSettingsDelegate)?.openFragment(R.string.temperature_unit)
        }

        humidityUnitLayout.setDebouncedOnClickListener {
            (activity as? AppSettingsDelegate)?.openFragment(R.string.humidity_unit)
        }

        pressureUnitLayout.setDebouncedOnClickListener {
            (activity as? AppSettingsDelegate)?.openFragment(R.string.pressure_unit)
        }

        debugToolsLayout.isVisible = BuildConfig.DEBUG
        debugToolsLayout.setDebouncedOnClickListener {
            StressTestGenerator.generateData(8, 15000, repository)
        }

        dashboardSwitch.isChecked = viewModel.isDashboardEnabled()
        dashboardSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsDashboardEnabled(isChecked)
        }

        updateView()
    }

    private fun updateView() {
        var intervalText = ""
        if (viewModel.getBackgroundScanMode() != BackgroundScanModes.DISABLED) {
            val bgScanInterval = viewModel.getBackgroundScanInterval()
            val min = bgScanInterval / 60
            val sec = bgScanInterval - min * 60
            if (min > 0) intervalText += min.toString() + " " + getString(R.string.minutes) + ", "
            intervalText += sec.toString() + " " + getString(R.string.seconds)
        }
        backgroundScanIntervalSubTextView.text = intervalText
        if (viewModel.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
            bgScanDescriptionTextView.text = getString(R.string.continuous_background_scanning_enabled, intervalText)
        } else {
            bgScanDescriptionTextView.text = getString(R.string.no_background_scanning_enabled)
        }

        gatewayUrlSubTextView.text = viewModel.getGatewayUrl()
        if (gatewayUrlSubTextView.text.isEmpty()) gatewayUrlSubTextView.text = getString(R.string.gateway_url_disabled)
        val temperature = viewModel.getTemperatureUnit()
        temperatureUnitSubTextView.text ="${getString(temperature.title)} (${getString(temperature.unit)})"
        val humidity = viewModel.getHumidityUnit()
        humidityUnitSubTextView.text = "${getString(humidity.title)} (${getString(humidity.unit)})"
        val pressure = viewModel.getPressureUnit()
        pressureUnitSubTextView.text = "${getString(pressure.title)} (${getString(pressure.unit)})"
    }
}
