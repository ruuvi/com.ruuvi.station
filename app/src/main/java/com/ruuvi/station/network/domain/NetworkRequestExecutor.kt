package com.ruuvi.station.network.domain

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.ruuvi.station.database.domain.NetworkRequestRepository
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.network.data.request.UnclaimSensorRequest
import com.ruuvi.station.network.data.request.UnshareSensorRequest
import com.ruuvi.station.network.data.request.UpdateSensorRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkRequestExecutor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkRequestRepository: NetworkRequestRepository
){
    private fun getToken() = tokenRepository.getTokenInfo()

    fun registerRequest(networkRequest: NetworkRequest) {
        networkRequestRepository.disableSimilar(networkRequest)
        execute(networkRequest)
    }

    fun executeScheduledRequests() {
        val requests = networkRequestRepository.getScheduledRequests()
        for (networkRequest in requests) {
            execute(networkRequest)
        }
    }
    
    private fun execute(networkRequest: NetworkRequest) {
        when (networkRequest.type) {
            NetworkRequestType.UNCLAIM -> unclaimSensor(networkRequest)
            NetworkRequestType.UPDATE_SENSOR -> updateSensor(networkRequest)
            NetworkRequestType.UNSHARE -> unshareSensor(networkRequest)
        }
    }

    private fun unshareSensor(networkRequest: NetworkRequest) {
        val token = getToken()?.token
        val request = parseJson<UnshareSensorRequest>(networkRequest.data)
        if (request != null) {
            token?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        networkRepository.unshareSensor(request, token)
                        disableRequest(networkRequest, NetworkRequestStatus.SUCCESS)
                    } catch (e: Exception) {
                        Timber.e(e, "unshareSensor")
                        registerFailedAttempt(networkRequest)
                    }
                }
            }
        } else {
            disableRequest(networkRequest, NetworkRequestStatus.PARSE_FAIL)
        }
    }

    private fun unclaimSensor(networkRequest: NetworkRequest) {
        val token = getToken()?.token
        val request = parseJson<UnclaimSensorRequest>(networkRequest.data)
        if (request != null) {
            token?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        networkRepository.unclaimSensor(request, token)
                        disableRequest(networkRequest, NetworkRequestStatus.SUCCESS)
                    } catch (e: Exception) {
                        Timber.e(e, "unclaimSensor")
                        registerFailedAttempt(networkRequest)
                    }
                }
            }
        } else {
            disableRequest(networkRequest, NetworkRequestStatus.PARSE_FAIL)
        }
    }

    private fun updateSensor(networkRequest: NetworkRequest) {
        val token = getToken()?.token

        val request = parseJson<UpdateSensorRequest>(networkRequest.data)
        if (request != null) {
            token?.let {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        networkRepository.updateSensor(request, token)
                        disableRequest(networkRequest, NetworkRequestStatus.SUCCESS)
                    } catch (e: Exception) {
                        Timber.e(e, "updateSensor")
                        registerFailedAttempt(networkRequest)
                    }
                }
            }
        } else {
            disableRequest(networkRequest, NetworkRequestStatus.PARSE_FAIL)
        }
    }

    private inline fun <reified T>parseJson(jsonString: String): T? {
        return try {
            Gson().fromJson(jsonString, T::class.java)
        } catch (e: Exception) {
            Timber.e(e)
            with (FirebaseCrashlytics.getInstance()){
                log("parseJson = $jsonString")
                recordException(e)
            }
            null
        }
    }

    private fun deleteRequest(networkRequest: NetworkRequest) {
        networkRequestRepository.delete(networkRequest)
    }

    private fun disableRequest(networkRequest: NetworkRequest, status: NetworkRequestStatus) {
        networkRequestRepository.disableRequest(networkRequest, status)
    }

    private fun registerFailedAttempt(networkRequest: NetworkRequest) {
        networkRequestRepository.registerFailedAttempt(networkRequest)
    }
}