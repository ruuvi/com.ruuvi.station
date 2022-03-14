package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor
import com.ruuvi.station.databinding.FragmentAppSettingsBackgroundScanBinding
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.ui.CustomNumberEdit
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class AppSettingsBackgroundScanFragment : Fragment(R.layout.fragment_app_settings_background_scan), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsBackgroundScanViewModel by viewModel()

    private lateinit var permissionsInteractor: PermissionsInteractor

    private lateinit var binding: FragmentAppSettingsBackgroundScanBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsBackgroundScanBinding.bind(view)
        permissionsInteractor = PermissionsInteractor(requireActivity())
        setupUI()
        setupViewModel()
    }

    private fun setupViewModel() {
        observeScanMode()
        observeInterval()
        observeShowOptimizationTips()
    }

    private fun setupUI() {
        with(binding) {
            var permissionAsked = false
            backgroundSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setBackgroundScanEnabled(isChecked)
                if (isChecked && !permissionAsked) {
                    permissionsInteractor.requestBackgroundPermission()
                    permissionAsked = true
                }
            }

            val list: MutableList<CustomNumberEdit.SelectionElement> = mutableListOf()
            list.add(CustomNumberEdit.SelectionElement(10, 10, R.string.background_interval_10sec))
            for (i in 1..60) {
                list.add(CustomNumberEdit.SelectionElement(60 * i, i, R.string.background_interval))
            }
            intervalEdit.elements = list
            intervalEdit.setOnValueChangedListener { value ->
                viewModel.setBackgroundScanInterval(value)
            }

            settingsInstructionsTextView.text =
                getString(viewModel.getBatteryOptimizationMessageId())

            openSettingsButton.setOnClickListener {
                viewModel.openOptimizationSettings()
            }
        }
    }

    private fun observeScanMode() {
        lifecycleScope.launch {
            viewModel.scanModeFlow.collect { mode ->
                val isEnabled = mode == BackgroundScanModes.BACKGROUND
                binding.backgroundSwitch.isChecked = isEnabled
                binding.intervalEdit.isEnabled = isEnabled
            }
        }
    }

    private fun observeInterval() {
        lifecycleScope.launch {
            viewModel.intervalFlow.collect {
                it?.let { interval ->
                    binding.intervalEdit.setSelectedItem(interval)
                }
            }
        }
    }

    private fun observeShowOptimizationTips() {
        lifecycleScope.launch {
            viewModel.showOptimizationTips.collect {
                binding.optimizationLayout.isVisible = it
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsBackgroundScanFragment()
    }
}