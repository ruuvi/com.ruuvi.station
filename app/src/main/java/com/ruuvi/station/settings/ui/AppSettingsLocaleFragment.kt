package com.ruuvi.station.settings.ui

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import androidx.fragment.app.Fragment
import com.ruuvi.station.R
import com.ruuvi.station.databinding.FragmentAppSettingsLocaleBinding
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsLocaleFragment : Fragment(R.layout.fragment_app_settings_locale), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsLocaleViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsLocaleBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsLocaleBinding.bind(view)
        setupUI()
    }

    private fun setupUI() {
        val items = viewModel.getAllTLocales()
        val current = viewModel.getLocale()

        items.forEachIndexed { index, option ->
            val radioButton = RadioButton(activity)
            radioButton.id = index
            radioButton.text = getString(option.title)
            radioButton.isChecked = option.code == current
            binding.radioGroup.addView(radioButton)
        }

        binding.radioGroup.setOnCheckedChangeListener { _, i ->
            viewModel.setLocale(items[i].code)
            //Lingver.getInstance().setLocale(requireContext(), Locale(items[i].code, items[i].country))
            activity?.finish()
            val intent = Intent(requireContext(), StartupActivity::class.java)
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
    }

    companion object {
        fun newInstance() = AppSettingsLocaleFragment()
    }
}