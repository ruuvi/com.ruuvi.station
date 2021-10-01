package com.ruuvi.station.settings.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.RadioButton
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentAppSettingsPressureBinding
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsPressureUnitFragment : Fragment(R.layout.fragment_app_settings_pressure), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsPressureUnitViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsPressureBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsPressureBinding.bind(view)
        setupUI()
    }

    private fun setupUI() {
        val items = viewModel.getAllPressureUnits()
        val current = viewModel.getPressureUnit()

        items.forEachIndexed { index, option ->
            val radioButton = RadioButton(activity)
            radioButton.id = index
            radioButton.text = getString(option.title)
            radioButton.isChecked = option == current
            binding.radioGroup.addView(radioButton)
        }

        binding.radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setPressureUnit(items[i])
        }
    }
    companion object {
        fun newInstance() = AppSettingsPressureUnitFragment()
    }
}