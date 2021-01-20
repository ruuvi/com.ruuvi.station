package com.ruuvi.station.settings.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.settings.domain.GatewayTestResultType
import com.ruuvi.station.util.extensions.makeWebLinks
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
        observeTestGatewayResult()
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

        settingsInfoTextView.makeWebLinks(requireActivity(), Pair(getString(R.string.settings_gateway_details_link), getString(R.string.settings_gateway_details_link_url)))

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

    fun observeTestGatewayResult() {
        lifecycleScope.launch {
            viewModel.observeTestGatewayResult.collect{
                when (it.type) {
                    GatewayTestResultType.NONE -> {
                        gatewayTestResultTextView.text = ""
                        gatewayTestResultTextView.setTextColor(Color.DKGRAY)
                    }
                    GatewayTestResultType.TESTING -> {
                        gatewayTestResultTextView.text = getString(R.string.gateway_testing)
                        gatewayTestResultTextView.setTextColor(Color.DKGRAY)
                    }
                    GatewayTestResultType.SUCCESS -> {
                        gatewayTestResultTextView.text = getString(R.string.gateway_test_success, it.code)
                        gatewayTestResultTextView.setTextColor(Color.GREEN)
                    }
                    GatewayTestResultType.FAIL -> {
                        gatewayTestResultTextView.text = getString(R.string.gateway_test_fail, it.code)
                        gatewayTestResultTextView.setTextColor(Color.RED)
                    }
                    GatewayTestResultType.EXCEPTION -> {
                        gatewayTestResultTextView.text = getString(R.string.gateway_test_exception)
                        gatewayTestResultTextView.setTextColor(Color.RED)
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsGatewayFragment()
    }
}