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
import com.flexsentlabs.extensions.viewModel
import com.koushikdutta.ion.Ion
import com.ruuvi.station.R
import com.ruuvi.station.model.HumidityUnit
import com.ruuvi.station.gateway.data.ScanEvent
import com.ruuvi.station.util.DeviceIdentifier
import kotlinx.android.synthetic.main.fragment_app_settings_detail.deviceIdentifierInput
import kotlinx.android.synthetic.main.fragment_app_settings_detail.deviceIdentifierLayout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.gatewayTestButton
import kotlinx.android.synthetic.main.fragment_app_settings_detail.gatewayTestResultTextView
import kotlinx.android.synthetic.main.fragment_app_settings_detail.gatewayTesterLayout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.inputLayout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.inputSettingEditText
import kotlinx.android.synthetic.main.fragment_app_settings_detail.inputSettingTitleTextView
import kotlinx.android.synthetic.main.fragment_app_settings_detail.radioGroup
import kotlinx.android.synthetic.main.fragment_app_settings_detail.radioLayout
import kotlinx.android.synthetic.main.fragment_app_settings_detail.radioSettingTitleTextView
import kotlinx.android.synthetic.main.fragment_app_settings_detail.settingsInfoTextView
import kotlinx.android.synthetic.main.fragment_app_settings_detail.wakelockLayoutContainer
import kotlinx.android.synthetic.main.fragment_app_settings_detail.wakelockSwitch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

private const val ARG_SETTING_RES = "arg_setting_res"

class AppSettingsDetailFragment : Fragment(R.layout.fragment_app_settings_detail), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsDetailViewModel by viewModel()

    private var res: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            res = it.getInt(ARG_SETTING_RES)
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.saveUrlAndDeviceId()
    }

    override fun onResume() {
        super.onResume()
        viewModel.restoreUrlAndDeviceId()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when (res) {
            R.string.temperature_unit -> {
                radioLayout.isVisible = true
                res?.let { radioSettingTitleTextView.text = getString(it) }
                val current = viewModel.getTemperatureUnit()
                val options = resources.getStringArray(R.array.list_preference_temperature_unit_titles)
                val values = resources.getStringArray(R.array.list_preference_temperature_unit_values)
                options.forEachIndexed { index, option ->
                    val radioButton = RadioButton(activity)
                    radioButton.id = index
                    radioButton.text = option
                    radioButton.isChecked = (values[index] == current)
                    radioGroup.addView(radioButton)
                }

                radioGroup.setOnCheckedChangeListener { _, i ->
                    viewModel.setTemperatureUnit(values[i])
                }
                settingsInfoTextView.text = getString(R.string.settings_temperature_unit_details)
            }
            R.string.humidity_unit -> {
                radioLayout.isVisible = true
                res?.let { radioSettingTitleTextView.text = getString(it) }
                val current = viewModel.getHumidityUnit()
                val options = resources.getStringArray(R.array.list_preference_humidity_unit_titles)
                val values = resources.getIntArray(R.array.list_preference_humidity_unit_values)
                options.forEachIndexed { index, option ->
                    val radioButton = RadioButton(activity)
                    radioButton.id = index
                    radioButton.text = option
                    radioButton.isChecked = (values[index] == current.code)
                    radioGroup.addView(radioButton)
                }

                radioGroup.setOnCheckedChangeListener { _, i ->
                    when (values[i]) {
                        0 -> viewModel.setHumidityUnit(HumidityUnit.PERCENT)
                        1 -> viewModel.setHumidityUnit(HumidityUnit.GM3)
                        2 -> viewModel.setHumidityUnit(HumidityUnit.DEW)
                    }
                }
                settingsInfoTextView.text = getString(R.string.settings_humidity_unit_details)
            }
            R.string.gateway_url -> {
                inputLayout.isVisible = true
                res?.let { inputSettingTitleTextView.text = getString(it) }
                inputSettingEditText.setText(viewModel.gatewayUrl)
                inputSettingEditText.hint = "https://your.gateway/..."
                inputSettingEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun afterTextChanged(p0: Editable?) {
                        viewModel.gatewayUrl = p0.toString()
                    }
                })
                //settingsInfoTextView.text = getString(R.string.settings_gateway_details)
                settingsInfoTextView.movementMethod = LinkMovementMethod.getInstance()
                deviceIdentifierLayout.isVisible = true
                deviceIdentifierInput.setText(viewModel.deviceId)
                deviceIdentifierInput.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun afterTextChanged(p0: Editable?) {
                        viewModel.deviceId = p0.toString()
                    }
                })
                wakelockLayoutContainer.isVisible = true
                wakelockSwitch.isChecked = viewModel.isServiceWakeLock()
                wakelockSwitch.setOnCheckedChangeListener { _, checked ->
                    viewModel.setIsServiceWakeLock(checked)
                }
                gatewayTesterLayout.isVisible = true
                gatewayTestButton.setDebouncedOnClickListener {
                    gatewayTestResultTextView.text = "Testing.."
                    gatewayTestResultTextView.setTextColor(Color.DKGRAY)
                    val scanEvent = ScanEvent(context)
                    scanEvent.deviceId = deviceIdentifierInput.text.toString()
                    Ion.with(context)
                        .load(inputSettingEditText.text.toString())
                        .setJsonPojoBody(scanEvent)
                        .asJsonObject()
                        .withResponse()
                        .setCallback { e, result ->
                            when {
                                e != null -> {
                                    gatewayTestResultTextView.setTextColor(Color.RED)
                                    gatewayTestResultTextView.text = "Nope, did not work. Is the URL correct?"
                                }
                                result.headers.code() != 200 -> {
                                    gatewayTestResultTextView.setTextColor(Color.RED)
                                    gatewayTestResultTextView.text = "Nope, did not work. Response code: " + result.headers.code()
                                }
                                else -> {
                                    gatewayTestResultTextView.setTextColor(Color.GREEN)
                                    gatewayTestResultTextView.text = "Gateway works! Response code: " + result.headers.code()
                                }
                            }
                        }
                }
            }
            R.string.device_identifier -> {
                inputLayout.isVisible = true
                res?.let { inputSettingTitleTextView.text = getString(it) }
                inputSettingEditText.setText(viewModel.deviceId)
                inputSettingEditText.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    }

                    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                        viewModel.deviceId = p0.toString()
                        if (p0.toString().isEmpty()) {
                            viewModel.deviceId = DeviceIdentifier.generateId()
                        }
                    }

                    override fun afterTextChanged(p0: Editable?) {
                    }
                })
                settingsInfoTextView.text = getString(R.string.settings_device_identifier_details)
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
