package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.koushikdutta.async.future.FutureCallback
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.GatewayTestResult
import com.ruuvi.station.settings.domain.GatewayTestResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettingsDataForwardingViewModel(
        private val interactor: AppSettingsInteractor
) : ViewModel() {
    private val dataForwardingUrl = MutableStateFlow(interactor.getDataForwardingUrl())
    val observeDataForwardingUrl: StateFlow<String> = dataForwardingUrl

    private val dataForwardingLocationEnabled = MutableStateFlow(interactor.getDataForwardingLocationEnabled())
    val observeDataForwardingLocationEnabled: StateFlow<Boolean> = dataForwardingLocationEnabled

    private val dataForwardingDuringSyncEnabled = MutableStateFlow(interactor.getDataForwardingDuringSyncEnabled())
    val observeDataForwardingDuringSyncEnabled: StateFlow<Boolean> = dataForwardingDuringSyncEnabled

    private val deviceId = MutableStateFlow(interactor.getDeviceId())
    val observeDeviceId: StateFlow<String> = deviceId

    private val testGatewayResult = MutableStateFlow(GatewayTestResult(GatewayTestResultType.NONE))
    val observeTestGatewayResult: StateFlow<GatewayTestResult> = testGatewayResult

    fun setDataForwardingUrl(url: String) {
        interactor.setDataForwardingUrl(url)
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
        testGatewayResult.value = GatewayTestResult(GatewayTestResultType.TESTING)
        interactor.testGateway(
                interactor.getDataForwardingUrl(),
                interactor.getDeviceId(),
                FutureCallback { e, result ->
                    when {
                        e != null -> {
                            testGatewayResult.value = GatewayTestResult(GatewayTestResultType.EXCEPTION)
                        }
                        result.headers.code() != 200 -> {
                            testGatewayResult.value = GatewayTestResult(GatewayTestResultType.FAIL, result.headers.code())
                        }
                        else -> {
                            testGatewayResult.value = GatewayTestResult(GatewayTestResultType.SUCCESS, result.headers.code())
                        }
                    }
                }
        )
    }
}