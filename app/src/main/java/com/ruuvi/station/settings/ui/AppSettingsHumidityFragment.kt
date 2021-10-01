package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RadioButton
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentAppSettingsHumidityBinding
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsHumidityFragment : Fragment(R.layout.fragment_app_settings_humidity), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsHumidityViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsHumidityBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsHumidityBinding.bind(view)
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
            binding.radioGroup.addView(radioButton)
        }

        binding.radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setHumidityUnit(items[i])
        }
    }
    companion object {
        fun newInstance() = AppSettingsHumidityFragment()
    }
}