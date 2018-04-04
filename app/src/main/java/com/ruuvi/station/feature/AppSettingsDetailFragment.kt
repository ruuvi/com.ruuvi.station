package com.ruuvi.station.feature

import android.os.Bundle
import android.app.Fragment
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton

import com.ruuvi.station.R
import com.ruuvi.station.util.DeviceIdentifier
import com.ruuvi.station.util.Utils
import kotlinx.android.synthetic.main.fragment_app_settings_detail.*

private const val ARG_SETTING_RES = "arg_setting_res"

class AppSettingsDetailFragment : Fragment() {
    lateinit var pref: SharedPreferences
    private var res: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            res = it.getInt(ARG_SETTING_RES)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_app_settings_detail, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (view == null) return

        pref = PreferenceManager.getDefaultSharedPreferences(activity)

        if (res == R.string.pref_bgscan) {
            scan_layout_container.visibility = View.VISIBLE
            (activity as AppSettingsActivity).setScanSwitchLayout(view)
            settings_info.text = "This will enable background scanning, meaning that you phone will periodically search for beacons even if you are not using it."
        } else if (res == R.string.pref_bgscan_battery_saving) {
            battery_layout_container.visibility = View.VISIBLE
            (activity as AppSettingsActivity).setBatterySwitchLayout(view)
        } else if (res == R.string.background_scan_interval) {
            radio_layout.visibility = View.VISIBLE
            radio_setting_title.text = getString(res!!)
            val current = (activity as AppSettingsActivity).getStringFromPref("pref_scaninterval", "30")
            val options = resources.getStringArray(R.array.pref_scaninterval_titles)
            val values = resources.getStringArray(R.array.pref_scaninterval_values)
            options.forEachIndexed { index, option ->
                val rb = RadioButton(activity)
                rb.id = Integer.parseInt(values[index])
                rb.text = option
                rb.isChecked = (values[index] == current)
                radio_group.addView(rb)
            }

            radio_group.setOnCheckedChangeListener { radioGroup, i ->
                pref.edit().putString("pref_scaninterval", radioGroup.checkedRadioButtonId.toString()).apply()
            }
        } else if (res == R.string.temperature_unit) {
            radio_layout.visibility = View.VISIBLE
            radio_setting_title.text = getString(res!!)
            val current = (activity as AppSettingsActivity).getStringFromPref("pref_temperature_unit", "C")
            val options = resources.getStringArray(R.array.list_preference_temperature_unit_titles)
            val values = resources.getStringArray(R.array.list_preference_temperature_unit_values)
            options.forEachIndexed { index, option ->
                val rb = RadioButton(activity)
                rb.id = index
                rb.text = option
                rb.isChecked = (values[index] == current)
                radio_group.addView(rb)
            }

            radio_group.setOnCheckedChangeListener { radioGroup, i ->
                pref.edit().putString("pref_temperature_unit", values[i]).apply()
            }
        } else if (res == R.string.gateway_url) {
            input_layout.visibility = View.VISIBLE
            input_setting_title.text = getString(res!!)
            input_setting.setText((activity as AppSettingsActivity).getStringFromPref("pref_backend", ""))
            input_setting.hint = "https://your.gateway/..."
            input_setting.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }

                override fun afterTextChanged(p0: Editable?) {
                    pref.edit().putString("pref_backend", p0.toString()).apply()
                }
            })
        } else if (res == R.string.device_identifier) {
            input_layout.visibility = View.VISIBLE
            input_setting_title.text = getString(res!!)
            input_setting.setText((activity as AppSettingsActivity).getStringFromPref("pref_device_id", ""))
            input_setting.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    pref.edit().putString("pref_device_id", p0.toString()).apply()
                    if (p0.toString().isEmpty()) {
                        // this will generate a new uuid and put it into the pref
                        DeviceIdentifier.id(activity)
                    }
                }

                override fun afterTextChanged(p0: Editable?) {
                }
            })
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(res: Int) =
                AppSettingsDetailFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_SETTING_RES, res)
                    }
                }
    }
}
