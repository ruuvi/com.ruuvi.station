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
import com.ruuvi.station.databinding.FragmentAppSettingsDataForwardingBinding
import com.ruuvi.station.dataforwarding.domain.LocationPermissionsInteractor
import com.ruuvi.station.settings.domain.GatewayTestResultType
import com.ruuvi.station.util.extensions.makeWebLinks
import com.ruuvi.station.util.extensions.resolveColorAttr
import com.ruuvi.station.util.extensions.setDebouncedOnClickListener
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.closestKodein
import timber.log.Timber

class AppSettingsDataForwardingFragment : Fragment(R.layout.fragment_app_settings_data_forwarding) , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: AppSettingsDataForwardingViewModel by viewModel()

    private lateinit var binding: FragmentAppSettingsDataForwardingBinding

    private var permissionsInteractor: LocationPermissionsInteractor? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentAppSettingsDataForwardingBinding.bind(view)
        permissionsInteractor = LocationPermissionsInteractor(requireActivity())
        setupUI()
        setupViewModel()
    }

    private fun setupViewModel() {
        observeDataForwardingUrl()
        observeDeviceId()
        observeTestGatewayResult()
        observeLocationEnabled()
        observeDataForwardingDuringSyncEnabled()
    }

    val requestLocationPermissionLauncher =
            registerForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions())
            { permissions ->
                if (permissions.any { it.value }) {
                    permissionsInteractor?.requestBackgroundLocationPermissionApi31(requestBackgroundLocationPermission)
                } else {
                    permissionsInteractor?.showLocationSnackbar()
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
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            locationSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDataForwardingLocationEnabled(isChecked)
                if (isChecked) {
                    permissionsInteractor?.requestLocationPermissionApi31(requestLocationPermissionLauncher, requestBackgroundLocationPermission)
                }
            }

            forwardingDuringSyncSwitch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.setDataForwardingDuringSyncEnabled(isChecked)
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
                ) {}

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
            }
        }
    }

    private fun observeDataForwardingDuringSyncEnabled() {
        lifecycleScope.launch {
            viewModel.observeDataForwardingDuringSyncEnabled.collect { isEnabled ->
                binding.forwardingDuringSyncSwitch.isChecked = isEnabled
            }
        }
    }

    private fun observeTestGatewayResult() {
        lifecycleScope.launch {
            val regularColor = requireActivity().resolveColorAttr(R.attr.colorPrimary)
            val errorColor = requireActivity().resolveColorAttr(R.attr.colorErrorText)
            val successColor = requireActivity().resolveColorAttr(R.attr.colorSuccessText)
            viewModel.observeTestGatewayResult.collect{
                with(binding) {
                    when (it.type) {
                        GatewayTestResultType.NONE -> {
                            gatewayTestResultTextView.text = ""
                            gatewayTestResultTextView.setTextColor(regularColor)
                        }
                        GatewayTestResultType.TESTING -> {
                            gatewayTestResultTextView.text = getString(R.string.gateway_testing)
                            gatewayTestResultTextView.setTextColor(regularColor)
                        }
                        GatewayTestResultType.SUCCESS -> {
                            gatewayTestResultTextView.text =
                                getString(R.string.gateway_test_success, it.code)
                            gatewayTestResultTextView.setTextColor(successColor)
                        }
                        GatewayTestResultType.FAIL -> {
                            gatewayTestResultTextView.text =
                                getString(R.string.gateway_test_fail, it.code)
                            gatewayTestResultTextView.setTextColor(errorColor)
                        }
                        GatewayTestResultType.EXCEPTION -> {
                            gatewayTestResultTextView.text =
                                getString(R.string.gateway_test_exception)
                            gatewayTestResultTextView.setTextColor(errorColor)
                        }
                    }
                }
            }
        }
    }

    companion object {
        fun newInstance() = AppSettingsDataForwardingFragment()
    }
}