package com.ruuvi.station.settings.ui

import android.graphics.Color
import androidx.lifecycle.ViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.koushikdutta.async.future.FutureCallback
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppSettingsGatewayViewModel(
        private val interactor: AppSettingsInteractor
) : ViewModel() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val gatewayUrl = MutableStateFlow(interactor.getGatewayUrl())
    val observeGatewayUrl: StateFlow<String> = gatewayUrl

    private val deviceId = MutableStateFlow(interactor.getDeviceId())
    val observeDeviceId: StateFlow<String> = deviceId

    private val testGatewayText = MutableStateFlow("")
    val observeTestGatewayText: StateFlow<String> = testGatewayText

    private val testGatewayColor = MutableStateFlow(Color.DKGRAY)
    val observeTestGatewayColor: StateFlow<Int> = testGatewayColor

    init {
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.setUserProperty("background_scan_enabled",
            (interactor.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND).toString())
        firebaseAnalytics.setUserProperty("background_scan_interval", interactor.getBackgroundScanInterval().toString())
        firebaseAnalytics.setUserProperty("gateway_enabled", interactor.getGatewayUrl().isNotEmpty().toString())
    }

    fun setGatewayUrl(newGatewayUrl: String) {
        interactor.setGatewayUrl(newGatewayUrl)
    }

    fun setDeviceId(newDeviceId: String) {
        interactor.setDeviceId(newDeviceId)
    }

    fun testGateway() {
        testGatewayColor.value = Color.DKGRAY
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