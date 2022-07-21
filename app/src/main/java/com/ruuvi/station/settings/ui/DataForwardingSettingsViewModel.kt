package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.koushikdutta.async.future.FutureCallback
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.GatewayTestResult
import com.ruuvi.station.settings.domain.GatewayTestResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DataForwardingSettingsViewModel(
        private val interactor: AppSettingsInteractor
) : ViewModel() {
    private val _dataForwardingUrl = MutableStateFlow(interactor.getDataForwardingUrl())
    val dataForwardingUrl: StateFlow<String> = _dataForwardingUrl

    private val _dataForwardingLocationEnabled = MutableStateFlow(interactor.getDataForwardingLocationEnabled())
    val dataForwardingLocationEnabled: StateFlow<Boolean> = _dataForwardingLocationEnabled

    private val _dataForwardingDuringSyncEnabled = MutableStateFlow(interactor.getDataForwardingDuringSyncEnabled())
    val dataForwardingDuringSyncEnabled: StateFlow<Boolean> = _dataForwardingDuringSyncEnabled

    private val _deviceId = MutableStateFlow(interactor.getDeviceId())
    val deviceId: StateFlow<String> = _deviceId

    private val _testGatewayResult = MutableStateFlow(GatewayTestResult(GatewayTestResultType.NONE))
    val testGatewayResult: StateFlow<GatewayTestResult> = _testGatewayResult

    fun setDataForwardingUrl(url: String) {
        interactor.setDataForwardingUrl(url)
        _dataForwardingUrl.value = interactor.getDataForwardingUrl()
    }

    fun setDataForwardingLocationEnabled(locationEnabled: Boolean) {
        interactor.setDataForwardingLocationEnabled(locationEnabled)
    }

    fun setDataForwardingDuringSyncEnabled(forwardingDuringSyncEnabled: Boolean) {
        interactor.setDataForwardingDuringSyncEnabled(forwardingDuringSyncEnabled)
    }

    fun setDeviceId(newDeviceId: String) {
        interactor.setDeviceId(newDeviceId)
    }

    fun testGateway() {
        _testGatewayResult.value = GatewayTestResult(GatewayTestResultType.TESTING)
        interactor.testGateway(
                interactor.getDataForwardingUrl(),
                interactor.getDeviceId(),
                FutureCallback { e, result ->
                    when {
                        e != null -> {
                            _testGatewayResult.value = GatewayTestResult(GatewayTestResultType.EXCEPTION)
                        }
                        result.headers.code() != 200 -> {
                            _testGatewayResult.value = GatewayTestResult(GatewayTestResultType.FAIL, result.headers.code())
                        }
                        else -> {
                            _testGatewayResult.value = GatewayTestResult(GatewayTestResultType.SUCCESS, result.headers.code())
                        }
                    }
                }
        )
    }
}