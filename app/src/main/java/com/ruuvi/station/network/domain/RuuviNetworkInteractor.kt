package com.ruuvi.station.network.domain

import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.requestWrappers.UploadImageRequestWrapper
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception
import java.util.*

class  RuuviNetworkInteractor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkRequestExecutor: NetworkRequestExecutor,
    private val sensorSettingsRepository: SensorSettingsRepository
) {
    val signedIn: Boolean
        get() = getToken() != null

    fun getEmail() = getToken()?.email

    private fun getToken() = tokenRepository.getTokenInfo()

    private var userInfo: UserInfoResponse? = null

    val mainScope = CoroutineScope(Dispatchers.Main)
    val ioScope = CoroutineScope(Dispatchers.IO)

    fun registerUser(user: UserRegisterRequest, onResult: (UserRegisterResponse?) -> Unit) {
        networkRepository.registerUser(user) {
            onResult(it)
        }
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        networkRepository.verifyUser(token) { response ->
            response?.let {
                if (response.error.isNullOrEmpty() && response.data != null) {
                    tokenRepository.saveTokenInfo(
                        NetworkTokenInfo(response.data.email, response.data.accessToken))
                }
            }
            onResult(response)
        }
    }

    fun getUserInfo(onResult: (UserInfoResponse?) -> Unit) {
        ioScope.launch {
            val result = getUserInfo()
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    suspend fun getUserInfo(): UserInfoResponse? {
        val token = getToken()
        if (token != null) {
            val benchUpdate1 = Date()
            userInfo = networkRepository.getUserInfo(token.token)
            val benchUpdate2 = Date()
            Timber.d("benchmark-getUserInfo-finish ${benchUpdate2.time - benchUpdate1.time} ms")
            return userInfo
        } else {
            return null
        }
    }

    fun getSensorNetworkStatus(mac: String): SensorDataResponse? {
        return userInfo?.data?.sensors?.firstOrNull {it.sensor == mac}
    }

    fun claimSensor(tag: RuuviTagEntity, onResult: (ClaimSensorResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            CoroutineScope(Dispatchers.IO).launch{
                val request = ClaimSensorRequest(tag.id.toString(), tag.displayName)
                try {
                    networkRepository.claimSensor(request, token) { claimResponse ->
                        if (claimResponse?.isSuccess() == true) {
                            sensorSettingsRepository.setSensorOwner(tag.id.toString(), getEmail()
                                ?: "")
                        }
                        getUserInfo {
                            onResult(claimResponse)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onResult(ClaimSensorResponse(RuuviNetworkResponse.errorResult, e.message.toString(), null))
                    }
                }
            }
        }
    }

    fun unclaimSensor(sensorId: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.UNCLAIM, sensorId, UnclaimSensorRequest(sensorId))
        Timber.d("unclaimSensor $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun unshareSensor(recipientEmail: String, sensorId: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.UNSHARE, sensorId, UnshareSensorRequest(recipientEmail, sensorId))
        Timber.d("unshareSensor $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun shareSensor(recipientEmail: String, tagId: String, handler: CoroutineExceptionHandler, onResult: (ShareSensorResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = ShareSensorRequest(recipientEmail, tagId)
                val response = networkRepository.shareSensor(request, token)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun unshareSensor(recipientEmail: String, tagId: String, handler: CoroutineExceptionHandler, onResult: (ShareSensorResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UnshareSensorRequest(recipientEmail, tagId)
                val response = networkRepository.unshareSensor(request, token)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun unshareAll(tagId: String, handler: CoroutineExceptionHandler, onResult: (Boolean) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val sharedInfo = networkRepository.getSharedSensors(it)
                if (sharedInfo?.data?.sensors?.isEmpty() == false) {
                    val emails = sharedInfo.data.sensors.filter { it.sensor == tagId }.map { it.sharedTo }
                    if (emails.isEmpty() == false) {
                        for (email in emails) {
                            val request = UnshareSensorRequest(email, tagId)
                            networkRepository.unshareSensor(request, token)
                        }
                        onResult(true)
                    }
                }
            }
        }
    }

    fun getShаredInfo(tagId: String, handler: CoroutineExceptionHandler, onResult: (List<SharedSensorDataResponse>?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val response = networkRepository.getSharedSensors(it)
                Timber.d("getShаredInfo ${response.toString()}")
                if (response?.data != null) {
                    val result = response.data.sensors.filter { it.sensor == tagId }
                    withContext(Dispatchers.Main) {
                        onResult(result)
                    }
                }
            }
        }
    }

    fun getSensorData(request: GetSensorDataRequest, onResult: (GetSensorDataResponse?) -> Unit) {
        val token = getToken()?.token
        mainScope.launch {
            token?.let {
                val result = networkRepository.getSensorData(token, request )
                onResult(result)
            }
        }
    }

    fun updateSensor(sensorId: String, name: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.UPDATE_SENSOR, sensorId, UpdateSensorRequest(sensorId, name))
        Timber.d("updateSensor $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun uploadImage(sensorId: String, filename: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.UPLOAD_IMAGE, sensorId, UploadImageRequestWrapper(filename, UploadImageRequest(sensorId)))
        Timber.d("uploadImage $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun uploadImage(tagId: String, filename: String, handler: CoroutineExceptionHandler, onResult: (UploadImageResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UploadImageRequest(tagId, "image/jpeg")
                val response = networkRepository.uploadImage(filename, request, token)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun resetImage(sensorId: String) {
        val networkRequest = NetworkRequest(NetworkRequestType.RESET_IMAGE, sensorId, UploadImageRequest.getResetImageRequest(sensorId))
        Timber.d("resetImage $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest)
    }

    fun resetImage(tagId: String, handler: CoroutineExceptionHandler, onResult: (UploadImageResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UploadImageRequest.getResetImageRequest(tagId)
                val response = networkRepository.resetImage(request, token)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    suspend fun getSensorData(request: GetSensorDataRequest):GetSensorDataResponse? = withContext(Dispatchers.IO) {
        val token = getToken()?.token
        token?.let {
            return@withContext networkRepository.getSensorData(token, request)
        }
    }
}
