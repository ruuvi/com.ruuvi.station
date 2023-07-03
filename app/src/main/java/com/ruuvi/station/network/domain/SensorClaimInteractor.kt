package com.ruuvi.station.network.domain

import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ContestSensorRequest
import com.ruuvi.station.network.data.request.UnclaimSensorRequest
import com.ruuvi.station.network.data.response.ClaimSensorResponse
import com.ruuvi.station.network.data.response.ContestSensorResponse
import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class SensorClaimInteractor(
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val networkResponseLocalizer: NetworkResponseLocalizer,
    private val ruuviNetworkInteractor: RuuviNetworkInteractor,
    private val alarmRepository: AlarmRepository,
) {
    fun getEmail() = getToken()?.email

    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun claimSensor(sensorId: String, name: String, onResult: (ClaimSensorResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            val request = ClaimSensorRequest(sensorId, name)
            try {
                networkRepository.claimSensor(request, token) { claimResponse ->
                    networkResponseLocalizer.localizeResponse(claimResponse)
                    if (claimResponse?.isSuccess() == true) {
                        sensorClaimed(sensorId)
                    } else {
                        tryToParseOwnerEmail(sensorId, claimResponse?.error ?: "")
                    }
                    onResult(claimResponse)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(
                        ClaimSensorResponse(
                            RuuviNetworkResponse.errorResult,
                            e.message.toString(),
                            null,
                            null
                        )
                    )
                }
            }
        }
    }

    suspend fun unclaimSensor(sensorId: String, onResult: (ClaimSensorResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            val request = UnclaimSensorRequest(sensorId)
            try {
                val response = networkRepository.unclaimSensor(request, token)
                tryToParseOwnerEmail(sensorId, response?.error ?: "")
                if (response?.isSuccess() == true) {
                    sensorSettingsRepository.setSensorOwner(sensorId, isNetworkSensor = false, owner = null)
                }
                onResult(response)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(
                        ClaimSensorResponse(
                            RuuviNetworkResponse.errorResult,
                            e.message.toString(),
                            null,
                            null
                        )
                    )
                }
            }
        }
    }

    suspend fun contestSensor(
        sensorId: String,
        name: String,
        secret: String,
        onResult: (ContestSensorResponse?) -> Unit
    ) {
        val token = getToken()?.token
        token?.let {
            val request = ContestSensorRequest(
                sensor = sensorId,
                secret = secret,
                name = name)
            try {
                networkRepository.contestSensor(request, token) { contestResponse ->
                    networkResponseLocalizer.localizeResponse(contestResponse)
                    if (contestResponse?.isSuccess() == true) {
                        sensorClaimed(sensorId)
                    } else {
                        tryToParseOwnerEmail(sensorId, contestResponse?.error ?: "")
                    }
                    onResult(contestResponse)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(
                        ContestSensorResponse(
                            RuuviNetworkResponse.errorResult,
                            e.message.toString(),
                            null,
                            null
                        )
                    )
                }
            }
        }
    }


    private fun tryToParseOwnerEmail(sensorId: String, errorMessage: String) {
        val maskedEmail =
            Regex("\\b\\S*@\\S*\\.\\S*\\b").find(errorMessage)?.value
        if (maskedEmail?.isNotEmpty() == true) {
            sensorSettingsRepository.setSensorOwner(
                sensorId,
                maskedEmail,
                false
            )
        }
    }

    private fun sensorClaimed(sensorId: String) {
        sensorSettingsRepository.setSensorOwner(
            sensorId,
            getEmail() ?: "",
            true
        )
        ruuviNetworkInteractor.updateSensorCalibration(sensorId)
        saveAlarmsToNetwork(sensorId)
        saveUserBackground(sensorId)
    }

    private fun saveAlarmsToNetwork(sensorId: String) {
        val alarms = alarmRepository.getForSensor(sensorId)
        for (alarm in alarms) {
            ruuviNetworkInteractor.setAlert(alarm)
        }
    }

    private fun saveUserBackground(sensorId: String) {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val userBackground = sensorSettings?.userBackground
        if (userBackground?.isNotEmpty() == true) {
            ruuviNetworkInteractor.uploadImage(
                sensorId = sensorId,
                filename = userBackground,
                uploadNow = true
            )
        }
    }
}