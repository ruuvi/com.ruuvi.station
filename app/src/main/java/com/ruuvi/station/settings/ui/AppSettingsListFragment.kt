package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.databinding.FragmentAppSettingsListBinding
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import org.kodein.di.generic.instance

class AppSettingsListFragment : Fragment(R.layout.fragment_app_settings_list), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsListViewModel by viewModel()

    private val repository: TagRepository by instance()

    private lateinit var binding: FragmentAppSettingsListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsListBinding.bind(view)
        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        viewModel.experimentalFeatures.observe(viewLifecycleOwner, Observer {
            binding.experimentalSettingsContainer.isVisible = false
        })
    }

    private fun setupUI() {
        with(binding) {
            scanLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_background_scan)
            }

            gatewaySettingsLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_data_forwarding)
            }

            graphSettingsLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_chart)
            }

            temperatureUnitLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_temperature_unit)
            }

            humidityUnitLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_humidity_unit)
            }

            pressureUnitLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_pressure_unit)
            }

            experimentalSettingsLayout.setDebouncedOnClickListener {
                (activity as? AppSettingsDelegate)?.openFragment(R.string.settings_experimental)
            }

            dashboardSwitch.isChecked = viewModel.isDashboardEnabled()
            dashboardSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setIsDashboardEnabled(isChecked)
            }

            val shouldShowCloudMode = viewModel.shouldShowCloudMode()
            cloudModeLayout.isVisible = shouldShowCloudMode
            networkModeDivider.divider.isVisible = shouldShowCloudMode
            cloudModeSwitch.isChecked = viewModel.isCloudModeEnabled()
            cloudModeSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setIsCloudModeEnabled(isChecked)
            }
        }
        updateView()
    }

    private fun updateView() {
        var intervalText = ""
        if (viewModel.getBackgroundScanMode() != BackgroundScanModes.DISABLED) {
            val bgScanInterval = viewModel.getBackgroundScanInterval()
            val min = bgScanInterval / 60
            val sec = bgScanInterval - min * 60
            if (min > 0) intervalText += min.toString() + " " + getString(R.string.min) + " "
            if (sec > 0) intervalText += sec.toString() + " " + getString(R.string.sec)
        }

        with(binding) {
            if (viewModel.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND) {
                bgScanDescriptionTextView.text =
                    getString(R.string.settings_background_continuous_description, intervalText)
            } else {
                bgScanDescriptionTextView.text =
                    getString(R.string.settings_background_disabled_description)
            }

            gatewayUrlSubTextView.text = viewModel.getGatewayUrl()
            if (gatewayUrlSubTextView.text.isEmpty()) gatewayUrlSubTextView.text =
                getString(R.string.data_forwarding_disabled)
            val temperature = viewModel.getTemperatureUnit()
            temperatureUnitSubTextView.text = getString(temperature.title)
            val humidity = viewModel.getHumidityUnit()
            humidityUnitSubTextView.text = getString(humidity.title)
            val pressure = viewModel.getPressureUnit()
            pressureUnitSubTextView.text = getString(pressure.title)
        }
    }
}
