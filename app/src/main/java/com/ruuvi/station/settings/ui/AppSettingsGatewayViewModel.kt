package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.koushikdutta.async.future.FutureCallback
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.GatewayTestResult
import com.ruuvi.station.settings.domain.GatewayTestResultType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettingsGatewayViewModel(
        private val interactor: AppSettingsInteractor
) : ViewModel() {
    private val gatewayUrl = MutableStateFlow(interactor.getGatewayUrl())
    val observeGatewayUrl: StateFlow<String> = gatewayUrl

    private val deviceId = MutableStateFlow(interactor.getDeviceId())
    val observeDeviceId: StateFlow<String> = deviceId

    private val testGatewayResult = MutableStateFlow(GatewayTestResult(GatewayTestResultType.NONE))
    val observeTestGatewayResult: StateFlow<GatewayTestResult> = testGatewayResult

    fun setGatewayUrl(newGatewayUrl: String) {
        interactor.setGatewayUrl(newGatewayUrl)
    }

    fun setDeviceId(newDeviceId: String) {
        interactor.setDeviceId(newDeviceId)
    }

    fun testGateway() {
        testGatewayResult.value = GatewayTestResult(GatewayTestResultType.TESTING)
        interactor.testGateway(
                interactor.getGatewayUrl(),
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