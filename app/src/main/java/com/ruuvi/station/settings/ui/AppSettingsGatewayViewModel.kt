package com.ruuvi.station.settings.ui

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.koushikdutta.async.future.FutureCallback
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettingsGatewayViewModel(
        private val interactor: AppSettingsInteractor
) : ViewModel() {
    private val gatewayUrl = MutableStateFlow(interactor.getGatewayUrl())
    val observeGatewayUrl: StateFlow<String> = gatewayUrl

    private val deviceId = MutableStateFlow(interactor.getDeviceId())
    val observeDeviceId: StateFlow<String> = deviceId

    private val testGatewayText = MutableStateFlow("")
    val observeTestGatewayText: StateFlow<String> = testGatewayText

    private val testGatewayColor = MutableStateFlow(Color.DKGRAY)
    val observeTestGatewayColor: StateFlow<Int> = testGatewayColor

    fun setGatewayUrl(newGatewayUrl: String) {
        interactor.setGatewayUrl(newGatewayUrl)
    }

    fun setDeviceId(newDeviceId: String) {
        interactor.setDeviceId(newDeviceId)
    }

    fun testGateway() {
        testGatewayColor.value = Color.DKGRAY
        //TODO FIX HARDCODED STRINGS HERE
        testGatewayText.value = "Testing.."
        interactor.testGateway(
                interactor.getGatewayUrl(),
                interactor.getDeviceId(),
                FutureCallback { e, result ->
                    when {
                        e != null -> {
                            testGatewayColor.value = Color.RED
                            testGatewayText.value = "Nope, did not work. Is the URL correct?"
                        }
                        result.headers.code() != 200 -> {
                            testGatewayColor.value = Color.RED
                            testGatewayText.value = "Nope, did not work. Response code: " + result.headers.code()
                        }
                        else -> {
                            testGatewayColor.value = Color.GREEN
                            testGatewayText.value = "Gateway works! Response code: " + result.headers.code()
                        }
                    }
                }
        )
    }
}