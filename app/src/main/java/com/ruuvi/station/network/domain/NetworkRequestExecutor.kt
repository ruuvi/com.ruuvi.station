package com.ruuvi.station.network.domain

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.ruuvi.station.database.domain.NetworkRequestRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.requestWrappers.UploadImageRequestWrapper
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class NetworkRequestExecutor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkRequestRepository: NetworkRequestRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val tagRepository: TagRepository
){
    private fun getToken() = tokenRepository.getTokenInfo()

    fun registerRequest(networkRequest: NetworkRequest, executeNow: Boolean = true) {
        CoroutineScope(Dispatchers.IO).launch {
            networkRequestRepository.disableSimilar(networkRequest)
            networkRequestRepository.saveRequest(networkRequest)
            if (executeNow) {
                execute(networkRequest)
            }
        }
    }

    suspend fun executeScheduledRequests() {
        val requests = networkRequestRepository.getScheduledRequests()
        for (networkRequest in requests) {
            execute(networkRequest)
        }
    }

    fun anySettingsRequests(): Boolean {
        val requests = networkRequestRepository.getScheduledRequests()
        return requests.any{ it.type == NetworkRequestType.SETTINGS }
    }

    private suspend fun execute(networkRequest: NetworkRequest) {
        val token = getToken()?.token

        val request = getRequest(networkRequest)

        if (request != null) {
            token?.let {
                try {
                    val response = runSpecificAction(token, networkRequest, request)
                    if (response?.isSuccess() == true) {
                        disableRequest(networkRequest, NetworkRequestStatus.SUCCESS)
                    } else {
                        registerFailedAttempt(networkRequest)
                    }
                } catch (e: Exception) {
                    registerFailedAttempt(networkRequest)
                }
            }
        } else {
            disableRequest(networkRequest, NetworkRequestStatus.PARSE_FAIL)
        }
    }

    private fun getRequest(networkRequest: NetworkRequest): Any? {
        with(networkRequest){
            return when (type) {
                NetworkRequestType.UNCLAIM -> parseJson<UnclaimSensorRequest>(requestData)
                NetworkRequestType.UPDATE_SENSOR -> parseJson<UpdateSensorRequest>(requestData)
                NetworkRequestType.UPLOAD_IMAGE -> parseJson<UploadImageRequestWrapper>(requestData)
                NetworkRequestType.SETTINGS -> parseJson<UpdateUserSettingRequest>(requestData)
                NetworkRequestType.UNSHARE -> parseJson<UnshareSensorRequest>(requestData)
                NetworkRequestType.RESET_IMAGE -> parseJson<UploadImageRequest>(requestData)
                NetworkRequestType.SET_ALERT -> parseJson<SetAlertRequest>(requestData)
            }
        }
    }

    private suspend fun runSpecificAction(token:String, networkRequest: NetworkRequest, request: Any?): RuuviNetworkResponse<*>? {
        return when (networkRequest.type) {
            NetworkRequestType.UNCLAIM -> unclaimSensor(token, request as UnclaimSensorRequest)
            NetworkRequestType.UPDATE_SENSOR -> updateSensor(token, request as UpdateSensorRequest)
            NetworkRequestType.UPLOAD_IMAGE -> uploadImage(token, request as UploadImageRequestWrapper)
            NetworkRequestType.SETTINGS -> updateUserSettings(token, request as UpdateUserSettingRequest)
            NetworkRequestType.UNSHARE -> unshareSensor(token, request as UnshareSensorRequest)
            NetworkRequestType.RESET_IMAGE -> resetImage(token, request as UploadImageRequest)
            NetworkRequestType.SET_ALERT -> setAlert(token, request as SetAlertRequest)
        }
    }

    private suspend fun setAlert(token: String, request: SetAlertRequest): SetAlertResponse? {
        return networkRepository.setAlert(request, token)
    }

    private suspend fun unclaimSensor(token: String, request: UnclaimSensorRequest): ClaimSensorResponse? {
        return networkRepository.unclaimSensor(request, token)
    }

    private suspend fun updateSensor(token: String, request: UpdateSensorRequest): UpdateSensorResponse? {
        return networkRepository.updateSensor(request, token)
    }

    private suspend fun unshareSensor(token: String, request: UnshareSensorRequest): ShareSensorResponse? {
        return networkRepository.unshareSensor(request, token)
    }

    private suspend fun uploadImage(token: String, request: UploadImageRequestWrapper): UploadImageResponse? {
        val response = networkRepository.uploadImage(request.filename, request.request, token)
        if (response?.isSuccess() == true && response.data?.guid.isNullOrEmpty()) {
            sensorSettingsRepository.updateNetworkBackground(request.request.sensor, response.data?.guid)
        }
        return response
    }

    private suspend fun resetImage(token: String, request: UploadImageRequest): UploadImageResponse? {
        return networkRepository.resetImage(request, token)
    }

    private suspend fun updateUserSettings(token: String, request: UpdateUserSettingRequest): UpdateUserSettingResponse? {
        return networkRepository.updateUserSettings(request, token)
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