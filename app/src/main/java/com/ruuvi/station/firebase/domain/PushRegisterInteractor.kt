package com.ruuvi.station.firebase.domain

import com.google.firebase.messaging.FirebaseMessaging
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class PushRegisterInteractor(
    val preferencesRepository: PreferencesRepository,
    val ruuviNetworkInteractor: RuuviNetworkInteractor,
) {
    fun checkAndRegisterDeviceToken() {
        Timber.d("checkAndRegisterDeviceToken")
        var registeredToken = preferencesRepository.getRegisteredToken()

        if (registeredToken.isNotEmpty() && !ruuviNetworkInteractor.signedIn) {
            CoroutineScope(Dispatchers.IO).launch {
                unregisterToken(registeredToken)
            }
        }

        if (ruuviNetworkInteractor.signedIn) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                Timber.d("checkAndRegisterDeviceToken - addOnCompleteListener $task")
                if (task.isSuccessful) {
                    val currentToken = task.result
                    registeredToken = preferencesRepository.getRegisteredToken()

                    if (registeredToken != currentToken) {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (registeredToken.isNotEmpty()) {
                                unregisterToken(registeredToken)
                            }

                            if (currentToken.isNotEmpty()) {
                                registerToken(currentToken)
                            }
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
}