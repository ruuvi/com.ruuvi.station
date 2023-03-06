package com.ruuvi.station.firebase.domain

import com.google.firebase.messaging.FirebaseMessaging
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.ui.model.ShareOperationStatus
import com.ruuvi.station.network.ui.model.ShareOperationType
import com.ruuvi.station.util.extensions.diffGreaterThan
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*

class PushRegisterInteractor(
    val preferencesRepository: PreferencesRepository,
    val ruuviNetworkInteractor: RuuviNetworkInteractor,
) {
    fun checkAndRegisterDeviceToken() {
        Timber.d("checkAndRegisterDeviceToken")
        var registeredToken = preferencesRepository.getRegisteredToken()

        if (registeredToken.isNotEmpty() && !ruuviNetworkInteractor.signedIn) {
            val handler = CoroutineExceptionHandler { _, exception ->
                Timber.e(exception)
            }

            CoroutineScope(Dispatchers.IO + handler).launch {
                unregisterToken(registeredToken)
            }
        }

        if (ruuviNetworkInteractor.signedIn) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                Timber.d("checkAndRegisterDeviceToken - addOnCompleteListener $task")
                if (task.isSuccessful) {
                    val currentToken = task.result
                    registeredToken = preferencesRepository.getRegisteredToken()

                    val lastRefresh = preferencesRepository.getDeviceTokenRefreshDate()

                    val tokenChanged = registeredToken != currentToken

                    val timeToRefreshToken = Date(lastRefresh).diffGreaterThan(refreshInterval)

                    CoroutineScope(Dispatchers.IO).launch {
                        Timber.d("checkAndRegisterDeviceToken tokenChanged = $tokenChanged timeToRefreshToken = $timeToRefreshToken")
                        if (tokenChanged && registeredToken.isNotEmpty()) {
                            unregisterToken(registeredToken)
                        }

                        if (currentToken.isNotEmpty() && (tokenChanged || timeToRefreshToken)) {
                            registerToken(currentToken)
                        }
                    }
                }
            }
        }
    }

    private suspend fun registerToken(currentToken: String) {
        Timber.d("registerToken $currentToken")
        val registerResult = ruuviNetworkInteractor.registerPush(currentToken)
        Timber.d("registerToken result $registerResult")
        if (registerResult?.isSuccess() == true) {
            preferencesRepository.updateRegisteredToken(currentToken)
            preferencesRepository.setDeviceTokenRefreshDate(Date().time)
        }
    }

    private suspend fun unregisterToken(registeredToken: String) {
        Timber.d("unregisterToken $registeredToken")
        val unregisterResult = ruuviNetworkInteractor.unregisterPush(registeredToken)
        Timber.d("unregisterToken result $unregisterResult")
        if (unregisterResult?.isSuccess() == true) {
            preferencesRepository.updateRegisteredToken("")
        }
    }

    companion object {
        private const val refreshInterval = 1000L * 3600 * 24 // 24h
    }
}