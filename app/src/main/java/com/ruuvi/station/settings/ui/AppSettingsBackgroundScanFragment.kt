package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RadioButton
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor
import kotlinx.android.synthetic.main.fragment_app_settings_background_scan.*
import kotlinx.android.synthetic.main.fragment_app_settings_background_scan.durationMinutesPicker
import kotlinx.android.synthetic.main.fragment_app_settings_background_scan.durationSecondsPicker
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsBackgroundScanFragment : Fragment(R.layout.fragment_app_settings_background_scan), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsBackgroundScanViewModel by viewModel()

    private lateinit var permissionsInteractor: PermissionsInteractor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        permissionsInteractor = PermissionsInteractor(requireActivity())
        setupViews()
        observeScanMode()
        observeInterval()
    }

    private fun setupViews() {
        durationMinutesPicker.minValue = 0
        durationMinutesPicker.maxValue = 59
        durationSecondsPicker.maxValue = 59

        viewModel.getPossibleScanModes().forEach { mode ->
            val radioButton = RadioButton(activity)
            radioButton.id = mode.value
            radioButton.text = getString(mode.label)
            optionsRadioGroup.addView(radioButton)
        }

        var permissionAsked = false
        optionsRadioGroup.setOnCheckedChangeListener { _, i ->
            val selection = BackgroundScanModes.fromInt(i)

            selection?.let {
                viewModel.setBackgroundMode(selection)
                settingsDescriptionTextView.text = getString(selection.description)
                if (selection == BackgroundScanModes.BACKGROUND && permissionAsked == false) {
                    permissionsInteractor.requestBackgroundPermission()
                    permissionAsked = true
                }
            }
        }

        durationMinutesPicker.setOnValueChangedListener { _, _, new ->
            if (new == 0) {
                durationSecondsPicker.minValue = 10
                if (durationSecondsPicker.value < 10) durationSecondsPicker.value = 10
            } else {
                durationSecondsPicker.minValue = 0
            }
            viewModel.setBackgroundScanInterval(new * 60 + durationSecondsPicker.value)
        }

        durationSecondsPicker.setOnValueChangedListener { _, _, new ->
            viewModel.setBackgroundScanInterval(durationMinutesPicker.value * 60 + new)
        }

        settingsInstructionsTextView.text = getString(viewModel.getBatteryOptimizationMessageId())
    }

    private fun observeScanMode() {
        lifecycleScope.launch {
            viewModel.scanModeFlow.collect { mode ->
                mode?.let {
                    optionsRadioGroup.check(it.value)
                }
            }
        }
    }

    private fun observeInterval() {
        lifecycleScope.launch {
            viewModel.intervalFlow.collect {
                it?.let { interval ->
                    val min = interval / 60
                    val sec = interval - min * 60

                    if (min == 0) durationSecondsPicker.minValue = 10

                    durationMinutesPicker.value = min
                    durationSecondsPicker.value = sec
                }
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsBackgroundScanFragment()
    }
}


