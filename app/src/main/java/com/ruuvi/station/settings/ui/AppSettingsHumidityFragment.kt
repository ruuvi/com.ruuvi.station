package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RadioButton
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import kotlinx.android.synthetic.main.fragment_app_settings_humidity.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsHumidityFragment : Fragment(R.layout.fragment_app_settings_humidity), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsHumidityViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        val items = viewModel.getAllHumidityUnits()
        val current = viewModel.getHumidityUnit()

        items.forEachIndexed { index, option ->
            val radioButton = RadioButton(activity)
            radioButton.id = index
            radioButton.text = getString(option.title)
            radioButton.isChecked = option == current
            radioGroup.addView(radioButton)
        }

        radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setHumidityUnit(items[i])
        }
    }
    companion object {
        fun newInstance() = AppSettingsHumidityFragment()
    }
}