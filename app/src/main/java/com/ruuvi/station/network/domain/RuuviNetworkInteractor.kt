package com.ruuvi.station.network.domain

import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

class  RuuviNetworkInteractor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository
) {
    val signedIn: Boolean
        get() = getToken() != null

    fun getEmail() = getToken()?.email

    private fun getToken() = tokenRepository.getTokenInfo()

    private var userInfo: UserInfoResponse? = null

    val mainScope = CoroutineScope(Dispatchers.Main)
    val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        getUserInfo {}
    }

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
            val request = ClaimSensorRequest(tag.displayName, tag.id.toString())
            networkRepository.claimSensor(request, token) { claimResponse ->
                getUserInfo {
                    onResult(claimResponse)
                }
            }
        }
    }

    fun unclaimSensor(tagId: String) {
        val token = getToken()?.token
        token?.let {
            ioScope.launch {
                networkRepository.unclaimSensor(UnclaimSensorRequest(tagId), token)
            }
        }
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

    fun updateSensor(tagId: String, newName: String, handler: CoroutineExceptionHandler, onResult: (UpdateSensorResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UpdateSensorRequest(tagId, newName)
                val response = networkRepository.updateSensor(request, token)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun uploadImage(tagId: String, filename: String, handler: CoroutineExceptionHandler, onResult: (UploadImageResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UploadImageRequest(tagId, "image/jpeg")
                val response = networkRepository.uploadImage(request, token)
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
