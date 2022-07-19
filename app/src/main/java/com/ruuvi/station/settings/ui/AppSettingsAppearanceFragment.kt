package com.ruuvi.station.settings.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.DarkModeState
import com.ruuvi.station.databinding.FragmentAppSettingsAppearanceBinding
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein

class AppSettingsAppearanceFragment: Fragment(R.layout.fragment_app_settings_appearance), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppearanceSettingsViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsAppearanceBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsAppearanceBinding.bind(view)
        setupViewModel()
        setupUI()
    }

    private fun setupUI() {
        binding.darkModeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.followSystemThemeRadioButton -> DarkModeState.SYSTEM_THEME
                R.id.darkThemeRadioButton -> DarkModeState.DARK_THEME
                R.id.lightThemeRadioButton -> DarkModeState.LIGHT_THEME
                else -> DarkModeState.SYSTEM_THEME
            }
            viewModel.setDarkMode(mode)
        }

        binding.dashboardSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIsDashboardEnabled(isChecked)
        }
    }

    private fun setupViewModel() {
        viewModel.darkMode.observe(viewLifecycleOwner) { darkMode ->
            when (darkMode) {
                DarkModeState.SYSTEM_THEME -> binding.followSystemThemeRadioButton.toggle()
                DarkModeState.DARK_THEME -> binding.darkThemeRadioButton.toggle()
                DarkModeState.LIGHT_THEME -> binding.lightThemeRadioButton.toggle()
            }
        }

        viewModel.dashboardEnabled.observe(viewLifecycleOwner) { dashboardEnabled ->
            binding.dashboardSwitch.isChecked = dashboardEnabled
        }
    }

    companion object {
        fun newInstance() = AppSettingsAppearanceFragment()
    }
}