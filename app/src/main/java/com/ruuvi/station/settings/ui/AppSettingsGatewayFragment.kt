package com.ruuvi.station.settings.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import kotlinx.android.synthetic.main.fragment_app_settings_gateway.*
import kotlinx.android.synthetic.main.fragment_app_settings_gateway.settingsInfoTextView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.support.closestKodein

class AppSettingsGatewayFragment : Fragment(R.layout.fragment_app_settings_gateway) , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsGatewayViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeGatewayUrl()
        observeDeviceId()
        observeTestGatewayText()
        observeTestGatewayColor()
    }

    private fun setupViews() {
        gatewayUrlEditText.addTextChangedListener( object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setGatewayUrl(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        deviceIdEditText.addTextChangedListener( object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                viewModel.setDeviceId(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        settingsInfoTextView.movementMethod = LinkMovementMethod.getInstance()

        gatewayTestButton.setDebouncedOnClickListener {
            viewModel.testGateway()
            gatewayTestResultTextView.visibility = View.VISIBLE
        }
    }

    fun observeGatewayUrl() {
        lifecycleScope.launch {
            viewModel.observeGatewayUrl.collect {
                gatewayUrlEditText.setText(it)
            }
        }
    }

    fun observeDeviceId() {
        lifecycleScope.launch {
            viewModel.observeDeviceId.collect {
                deviceIdEditText.setText(it)
            }
        }
    }

    fun observeTestGatewayText() {
        lifecycleScope.launch {
            viewModel.observeTestGatewayText.collect{
                gatewayTestResultTextView.text = it
            }
        }
    }

    fun observeTestGatewayColor() {
        lifecycleScope.launch {
            viewModel.observeTestGatewayColor.collect{
                gatewayTestResultTextView.setTextColor(it)
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsGatewayFragment()
    }
}