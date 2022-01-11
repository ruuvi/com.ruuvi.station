package com.ruuvi.station.settings.ui

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor
import com.ruuvi.station.databinding.FragmentAppSettingsGatewayBinding
import com.ruuvi.station.settings.domain.GatewayTestResultType
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import timber.log.Timber

class AppSettingsGatewayFragment : Fragment(R.layout.fragment_app_settings_gateway) , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsGatewayViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsGatewayBinding

    private var permissionsInteractor: PermissionsInteractor? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsGatewayBinding.bind(view)
        permissionsInteractor = PermissionsInteractor(requireActivity())
        setupUI()
        setupViewModel()
    }

    private fun setupViewModel() {
        observeDataForwardingUrl()
        observeDeviceId()
        observeTestGatewayResult()
        observeLocationEnabled()
    }

    val requestLocationPermissionLauncher =
            registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions())
            { permissions ->
                if (permissions.any { it.value }) {
                    permissionsInteractor?.requestBackgroundLocationPermissionApi31(requestBackgroundLocationPermission)
                }
                permissions.entries.forEach {
                    Timber.d("${it.key} granted = ${it.value}")
                }
            }

    val requestBackgroundLocationPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    // result currently not used
                }
            }

    private fun setupUI() {
        with(binding) {
            gatewayUrlEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setDataForwardingUrl(s.toString())
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            locationSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDataForwardingLocationEnabled(isChecked)
            }

            deviceIdEditText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setDeviceId(s.toString())
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            settingsInfoTextView.makeWebLinks(
                requireActivity(),
                Pair(
                    getString(R.string.settings_gateway_details_link),
                    getString(R.string.settings_gateway_details_link_url)
                )
            )

            gatewayTestButton.setDebouncedOnClickListener {
                viewModel.testGateway()
                gatewayTestResultTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun observeDataForwardingUrl() {
        lifecycleScope.launch {
            viewModel.observeDataForwardingUrl.collect {
                binding.gatewayUrlEditText.setText(it)
            }
        }
    }

    private fun observeDeviceId() {
        lifecycleScope.launch {
            viewModel.observeDeviceId.collect {
                binding.deviceIdEditText.setText(it)
            }
        }
    }

    private fun observeLocationEnabled() {
        lifecycleScope.launch {
            viewModel.observeDataForwardingLocationEnabled.collect { isEnabled ->
                binding.locationSwitch.isChecked = isEnabled
                if (isEnabled) {
                    permissionsInteractor?.requestLocationPermissionApi31(requestLocationPermissionLauncher, requestBackgroundLocationPermission)
                }
            }
        }
    }

    private fun observeTestGatewayResult() {
        lifecycleScope.launch {
            viewModel.observeTestGatewayResult.collect{
                with(binding) {
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
                            gatewayTestResultTextView.text =
                                getString(R.string.gateway_test_success, it.code)
                            gatewayTestResultTextView.setTextColor(Color.GREEN)
                        }
                        GatewayTestResultType.FAIL -> {
                            gatewayTestResultTextView.text =
                                getString(R.string.gateway_test_fail, it.code)
                            gatewayTestResultTextView.setTextColor(Color.RED)
                        }
                        GatewayTestResultType.EXCEPTION -> {
                            gatewayTestResultTextView.text =
                                getString(R.string.gateway_test_exception)
                            gatewayTestResultTextView.setTextColor(Color.RED)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsGatewayFragment()
    }
}