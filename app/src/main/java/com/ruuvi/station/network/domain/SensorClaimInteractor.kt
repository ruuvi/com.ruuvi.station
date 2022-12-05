package com.ruuvi.station.network.domain

import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ContestSensorRequest
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
    private val networkResponseLocalizer: NetworkResponseLocalizer
) {
    fun getEmail() = getToken()?.email

    private fun getToken() = tokenRepository.getTokenInfo()

    suspend fun claimSensor(sensorSettings: SensorSettings, onResult: (ClaimSensorResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            val request = ClaimSensorRequest(sensorSettings.id, sensorSettings.displayName)
            try {
                networkRepository.claimSensor(request, token) { claimResponse ->
                    networkResponseLocalizer.localizeResponse(claimResponse)
                    if (claimResponse?.isSuccess() == true) {
                        sensorSettingsRepository.setSensorOwner(
                            sensorSettings.id,
                            getEmail() ?: "",
                            true
                        )
                    } else {
                        val maskedEmail =
                            Regex("\\b\\S*@\\S*\\.\\S*\\b").find(claimResponse?.error ?: "")?.value
                        if (maskedEmail?.isNotEmpty() == true) {
                            sensorSettingsRepository.setSensorOwner(
                                sensorSettings.id,
                                maskedEmail,
                                false
                            )
                        }
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
                        sensorSettingsRepository.setSensorOwner(
                            sensorId,
                            getEmail() ?: "",
                            true
                        )
                    } else {
                        val maskedEmail =
                            Regex("\\b\\S*@\\S*\\.\\S*\\b").find(contestResponse?.error ?: "")?.value
                        if (maskedEmail?.isNotEmpty() == true) {
                            sensorSettingsRepository.setSensorOwner(
                                sensorId,
                                maskedEmail,
                                false
                            )
                        }
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
}