package com.ruuvi.station.settings.ui

import androidx.lifecycle.Observer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.android.synthetic.main.fragment_app_settings_background_scan.*
import kotlinx.android.synthetic.main.fragment_app_settings_background_scan.duration_minute
import kotlinx.android.synthetic.main.fragment_app_settings_background_scan.duration_second
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsBackgroundScanFragment : Fragment(R.layout.fragment_app_settings_background_scan), KodeinAware {
    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsBackgroundScanViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        observeScanMode()
        observeInterval()
    }

    private fun setupViews() {
        duration_minute.minValue = 0
        duration_minute.maxValue = 59
        duration_second.maxValue = 59

        viewModel.getPossibleScanModes().forEach { mode ->
            val rb = RadioButton(activity)
            rb.id = mode.value
            rb.text = getString(mode.label)
            optionsRadioGroup.addView(rb)
        }

        optionsRadioGroup.setOnCheckedChangeListener { _, i ->
            val selection = BackgroundScanModes.fromInt(i)

            selection?.let {
                viewModel.setBackgroundMode(selection)
                settingsDescriptionTextView.text = getString(selection.description)
            }
        }

        duration_minute.setOnValueChangedListener { _, _, new ->
            if (new == 0) {
                duration_second.minValue = 10
                if (duration_second.value < 10) duration_second.value = 10
            } else {
                duration_second.minValue = 0
            }
            viewModel.setBackgroundScanInterval(new * 60 + duration_second.value)
        }

        duration_second.setOnValueChangedListener { _, _, new ->
            viewModel.setBackgroundScanInterval(duration_minute.value * 60 + new)
        }

        settingsInstructionsTextView.text = getString(viewModel.getBatteryOptimizationMessageId())
    }

    private fun observeScanMode() {
        viewModel.observeScanMode().observe(viewLifecycleOwner, Observer {
            it?.let {
                optionsRadioGroup.check(it.value)
            }
        })
    }

    private fun observeInterval() {
        viewModel.observeInterval().observe(viewLifecycleOwner, Observer {
            it?.let { interval ->
                val min = interval / 60
                val sec = interval - min * 60

                if (min == 0) duration_second.minValue = 10

                duration_minute.value = min
                duration_second.value = sec
            }
        })
    }

    companion object {
        fun newInstance() = AppSettingsBackgroundScanFragment()
    }
}


