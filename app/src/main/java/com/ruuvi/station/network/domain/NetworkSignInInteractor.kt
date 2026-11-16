package com.ruuvi.station.network.domain

import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.database.domain.NetworkRequestRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.firebase.domain.PushRegisterInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetworkSignInInteractor (
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val networkTokenRepository: NetworkTokenRepository,
    private val networkRequestRepository: NetworkRequestRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val pushRegisterInteractor: PushRegisterInteractor,
    private val preferencesRepository: PreferencesRepository,
) {
    fun signIn(token: String, email: String, onResult: (UiText?) -> Unit) {
        networkInteractor.verifyUser(token, email) { response ->
            val error = response?.error
            if (response?.isSuccess() == true && response.data != null) {
                pushRegisterInteractor.checkAndRegisterDeviceToken()
                onResult(null)
            } else if (error.isNullOrEmpty()) {
                onResult(UiText.StringResource(R.string.unknown_error))
            } else {
                onResult(UiText.DynamicString(error))
            }
        }
    }

    fun signOut(finished: ()->Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val stopJob = networkDataSyncInteractor.stopSync {
                networkRequestRepository.deletePendingRequests()
                networkTokenRepository.clearTokenInfo()

                preferencesRepository.resetSubscriptionShareSettings()
                preferencesRepository.resetAppSettings()
                preferencesRepository.clearMarketingConsent()
                pushRegisterInteractor.checkAndRegisterDeviceToken()

                val sensors = sensorSettingsRepository.getSensorSettings()
                for (sensor in sensors) {
                    if (sensor.networkSensor) {
                        tagRepository.deleteSensorAndRelatives(sensor.id)
                    }
                }
            }
            stopJob.join()
            withContext(Dispatchers.Main){
                finished()
            }
        }
    }
}
