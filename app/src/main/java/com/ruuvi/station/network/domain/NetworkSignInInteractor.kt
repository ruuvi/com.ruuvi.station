package com.ruuvi.station.network.domain

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
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val pushRegisterInteractor: PushRegisterInteractor
) {
    fun signIn(token: String, response: (String) -> Unit) {
        networkInteractor.verifyUser(token) {response->
            if (response?.isSuccess() == true) {
                pushRegisterInteractor.checkAndRegisterDeviceToken()
            }

            var  errorText = ""
            // TODO LOCALIZE
            if (response == null) {
                errorText = "Unknown error"
            } else if (!response.error.isNullOrEmpty()) {
                errorText = response.error
            }
            response(errorText)
        }
    }

    fun signOut(finished: ()->Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val stopJob = networkDataSyncInteractor.stopSync()
            stopJob.join()

            networkTokenRepository.clearTokenInfo()
            pushRegisterInteractor.checkAndRegisterDeviceToken()

            val sensors = sensorSettingsRepository.getSensorSettings()
            for (sensor in sensors) {
                if (sensor.networkSensor) {
                    tagRepository.deleteSensorAndRelatives(sensor.id)
                }
            }
            withContext(Dispatchers.Main){
                finished()
            }
        }
    }
}