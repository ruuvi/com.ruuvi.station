package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RadioButton
import com.flexsentlabs.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_app_settings_temperature.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsTemperatureUnitFragment : Fragment(R.layout.fragment_app_settings_temperature), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsTemperatureUnitViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        val items = viewModel.getAllTemperatureUnits()
        val current = viewModel.getTemperatureUnit()

        items.forEachIndexed { index, option ->
            val radioButton = RadioButton(activity)
            radioButton.id = index
            radioButton.text = "${getString(option.title)} (${getString(option.unit)})"
            radioButton.isChecked = option == current
            radioGroup.addView(radioButton)
        }

        radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setTemperatureUnit(items[i])
        }
    }
    companion object {
        fun newInstance() = AppSettingsTemperatureUnitFragment()
    }
}