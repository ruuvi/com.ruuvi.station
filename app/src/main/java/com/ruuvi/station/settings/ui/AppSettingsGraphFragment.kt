package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentAppSettingsGraphBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class AppSettingsGraphFragment : Fragment(R.layout.fragment_app_settings_graph), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsGraphViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsGraphBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsGraphBinding.bind(view)
        setupViews()
        setupViewModel()
    }

    private fun setupViewModel() {
        viewModel.startEdit()
        observeInterval()
        observePeriod()
        observeShowAllPoints()
        observeDrawDots()
    }

    override fun onStop() {
        super.onStop()
        viewModel.stopEdit()
    }

    private fun setupViews() {
        with(binding) {
            graphIntervalNumberPicker.minValue = 1
            graphIntervalNumberPicker.maxValue = 60

            graphIntervalNumberPicker.setOnValueChangedListener { _, _, new ->
                viewModel.setPointInterval(new)
            }

            chartViewPeriodEdit.setOnValueChangedListener { period ->
                viewModel.setViewPeriod(period)
            }

            graphAllPointsSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setShowAllPoints(isChecked)
            }

            graphDrawDotsSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDrawDots(isChecked)
            }
        }
    }

    private fun observeInterval() {
        lifecycleScope.launch {
            viewModel.observePointInterval().collect {
                binding.graphIntervalNumberPicker.value = it
            }
        }
    }

    private fun observePeriod() {
        lifecycleScope.launch {
            viewModel.observeViewPeriod().collect {
                binding.chartViewPeriodEdit.selectedValue = it
            }
        }
    }

    private fun observeShowAllPoints() {
        lifecycleScope.launch {
            viewModel.showAllPointsFlow.collect {
                with(binding) {
                    graphAllPointsSwitch.isChecked = it
                    graphIntervalNumberPicker.isEnabled = !graphAllPointsSwitch.isChecked
                }
            }
        }
    }

    private fun observeDrawDots() {
        lifecycleScope.launch {
            viewModel.drawDotsFlow.collect {
                binding.graphDrawDotsSwitch.isChecked = it
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsGraphFragment()
    }
}