package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import androidx.lifecycle.Observer
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentAppSettingsExperimentalBinding
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class AppSettingsExperimentalFragment : Fragment(R.layout.fragment_app_settings_experimental) , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsExperimentalViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsExperimentalBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsExperimentalBinding.bind(view)
        setupViewModel()
        setupUI()
    }

    private fun setupViewModel() {
        viewModel.ruuviNetworkEnabledObserve.observe(viewLifecycleOwner, Observer {
            binding.ruuviNetworkSwitch.isChecked = it
        })
    }

    private fun setupUI() {
        binding.ruuviNetworkSwitch.setOnCheckedChangeListener {_, checked ->
            viewModel.setRuuviNetworkEnabled(checked)
        }
    }

    companion object {
        fun newInstance() = AppSettingsExperimentalFragment()
    }
}