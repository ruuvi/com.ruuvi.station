package com.ruuvi.station.settings.ui

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.RadioButton
import androidx.core.view.isVisible
import com.flexsentlabs.androidcommons.app.ui.setDebouncedOnClickListener
import com.koushikdutta.ion.Ion
import com.ruuvi.station.R
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.gateway.data.ScanEvent
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.util.DeviceIdentifier
import kotlinx.android.synthetic.main.fragment_app_settings_detail.device_identifier_input
import kotlinx.android.synthetic.main.fragment_app_settings_detail.device_identifier_layout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.gateway_test_button
import kotlinx.android.synthetic.main.fragment_app_settings_detail.gateway_test_result
import kotlinx.android.synthetic.main.fragment_app_settings_detail.gateway_tester_layout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.input_layout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.input_setting
import kotlinx.android.synthetic.main.fragment_app_settings_detail.input_setting_title
import kotlinx.android.synthetic.main.fragment_app_settings_detail.radio_group
import kotlinx.android.synthetic.main.fragment_app_settings_detail.radio_layout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.radio_setting_title
import kotlinx.android.synthetic.main.fragment_app_settings_detail.settings_info
import kotlinx.android.synthetic.main.fragment_app_settings_detail.wakelock_layout_container
import kotlinx.android.synthetic.main.fragment_app_settings_detail.wakelock_switch

private const val ARG_SETTING_RES = "arg_setting_res"

class AppSettingsDetailFragment : Fragment(R.layout.fragment_app_settings_detail) {
    private var res: Int? = null
    lateinit var prefs: Preferences
    private var gatewayUrl = ""
    private var deviceId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Preferences(this.requireContext())
        arguments?.let {
            res = it.getInt(ARG_SETTING_RES)
        }
    }

    override fun onPause() {
        super.onPause()
        prefs.gatewayUrl = gatewayUrl
        prefs.deviceId = deviceId
        //MainActivity.setBackgroundScanning(activity)
    }

    override fun onResume() {
        super.onResume()
        if (gatewayUrl.isEmpty()) gatewayUrl = prefs.gatewayUrl
        if (deviceId.isEmpty()) deviceId = prefs.deviceId
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (res) {
            R.string.temperature_unit -> {
                radio_layout.isVisible = true
                res?.let { radio_setting_title.text = getString(it) }
                val current = prefs.temperatureUnit
                val options = resources.getStringArray(R.array.list_preference_temperature_unit_titles)
                val values = resources.getStringArray(R.array.list_preference_temperature_unit_values)
                options.forEachIndexed { index, option ->
                    val radioButton = RadioButton(activity)
                    radioButton.id = index
                    radioButton.text = option
                    radioButton.isChecked = (values[index] == current)
                    radio_group.addView(radioButton)
                }

                radio_group.setOnCheckedChangeListener { _, i ->
                    prefs.temperatureUnit = values[i]
                }
                settings_info.text = getString(R.string.settings_temperature_unit_details)
            }
            R.string.humidity_unit -> {
                radio_layout.isVisible = true
                res?.let { radio_setting_title.text = getString(it) }
                val current = prefs.humidityUnit
                val options = resources.getStringArray(R.array.list_preference_humidity_unit_titles)
                val values = resources.getIntArray(R.array.list_preference_humidity_unit_values)
                options.forEachIndexed { index, option ->
                    val radioButton = RadioButton(activity)
                    radioButton.id = index
                    radioButton.text = option
                    radioButton.isChecked = (values[index] == current.code)
                    radio_group.addView(radioButton)
                }

                radio_group.setOnCheckedChangeListener { _, i ->
                    when (values[i]) {
                        0 -> prefs.humidityUnit = HumidityUnit.PERCENT
                        1 -> prefs.humidityUnit = HumidityUnit.GM3
                        2 -> prefs.humidityUnit = HumidityUnit.DEW
                    }
                }
                settings_info.text = getString(R.string.settings_humidity_unit_details)
            }
            R.string.gateway_url -> {
                input_layout.isVisible = true
                res?.let { input_setting_title.text = getString(it) }
                input_setting.setText(prefs.gatewayUrl)
                input_setting.hint = "https://your.gateway/..."
                input_setting.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun afterTextChanged(p0: Editable?) {
                        gatewayUrl = p0.toString()
                    }
                })
                //settings_info.text = getString(R.string.settings_gateway_details)
                settings_info.movementMethod = LinkMovementMethod.getInstance()
                device_identifier_layout.isVisible = true
                device_identifier_input.setText(prefs.deviceId)
                device_identifier_input.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun afterTextChanged(p0: Editable?) {
                        deviceId = p0.toString()
                    }
                })
                wakelock_layout_container.isVisible = true
                wakelock_switch.isChecked = prefs.serviceWakelock
                wakelock_switch.setOnCheckedChangeListener { _, checked ->
                    prefs.serviceWakelock = checked
                }
                gateway_tester_layout.isVisible = true
                gateway_test_button.setDebouncedOnClickListener {
                    gateway_test_result.visibility = View.VISIBLE
                    gateway_test_result.text = "Testing.."
                    gateway_test_result.setTextColor(Color.DKGRAY)
                    val scanEvent = ScanEvent(context)
                    scanEvent.deviceId = device_identifier_input.text.toString()
                    Ion.with(context)
                        .load(input_setting.text.toString())
                        .setJsonPojoBody(scanEvent)
                        .asJsonObject()
                        .withResponse()
                        .setCallback { e, result ->
                            when {
                                e != null -> {
                                    gateway_test_result.setTextColor(Color.RED)
                                    gateway_test_result.text = "Nope, did not work. Is the URL correct?"
                                }
                                result.headers.code() != 200 -> {
                                    gateway_test_result.setTextColor(Color.RED)
                                    gateway_test_result.text = "Nope, did not work. Response code: " + result.headers.code()
                                }
                                else -> {
                                    gateway_test_result.setTextColor(Color.GREEN)
                                    gateway_test_result.text = "Gateway works! Response code: " + result.headers.code()
                                }
                            }
                        }
                }
            }
            R.string.device_identifier -> {
                input_layout.isVisible = true
                res?.let { input_setting_title.text = getString(it) }
                input_setting.setText(prefs.deviceId)
                input_setting.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        deviceId = p0.toString()
                        if (p0.toString().isEmpty()) {
                            deviceId = DeviceIdentifier.generateId()
                        }
                    }

                    override fun afterTextChanged(p0: Editable?) {
                    }
                })
                settings_info.text = getString(R.string.settings_device_identifier_details)
            }
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
