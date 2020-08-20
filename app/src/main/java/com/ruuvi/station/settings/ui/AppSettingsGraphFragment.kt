package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_app_settings_graph.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsGraphFragment : Fragment(R.layout.fragment_app_settings_graph), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsGraphViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeInterval()
        observePeriod()
        observeShowAllPoints()
    }

    private fun setupViews() {
        graphIntervalNumberPicker.minValue = 1
        graphIntervalNumberPicker.maxValue = 60
        viewPeriodNumberPicker.minValue = 1
        viewPeriodNumberPicker.maxValue = 72

        graphIntervalNumberPicker.setOnValueChangedListener { _, _, new ->
            viewModel.setPointInterval(new)
        }

        viewPeriodNumberPicker.setOnValueChangedListener { _, _, new ->
            viewModel.setViewPeriod(new)
        }

        graphAllPointsSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setShowAllPoints(isChecked)
        }
    }

    private fun observeInterval() {
        lifecycleScope.launch {
            viewModel.observePointInterval().collect {
                graphIntervalNumberPicker.value = it
            }
        }
    }

    private fun observePeriod() {
        lifecycleScope.launch {
            viewModel.observeViewPeriod().collect {
                viewPeriodNumberPicker.value = it
            }
        }
    }

    private fun observeShowAllPoints() {
        lifecycleScope.launch {
            viewModel.showAllPointsFlow.collect {
                graphAllPointsSwitch.isChecked = it
                graphIntervalNumberPicker.isEnabled = !graphAllPointsSwitch.isChecked
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsGraphFragment()
    }
}