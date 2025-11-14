package com.ruuvi.station.network.domain

import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.model.NetworkRequestType
import com.ruuvi.station.database.tables.Alarm
import com.ruuvi.station.database.tables.NetworkRequest
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.network.data.NetworkTokenInfo
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.requestWrappers.UploadImageRequestWrapper
import com.ruuvi.station.network.data.response.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.lang.Exception
import java.util.*

class RuuviNetworkInteractor (
    private val tokenRepository: NetworkTokenRepository,
    private val networkRepository: RuuviNetworkRepository,
    private val networkRequestExecutor: NetworkRequestExecutor,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val firebaseInteractor: FirebaseInteractor,
    private val networkResponseLocalizer: NetworkResponseLocalizer
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
            networkResponseLocalizer.localizeResponse(it)
            onResult(it)
        }
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        networkRepository.verifyUser(token) { response ->
            networkResponseLocalizer.localizeResponse(response)
            response?.let {
                if (response.error.isNullOrEmpty() && response.data != null) {
                    tokenRepository.saveTokenInfo(
                        NetworkTokenInfo(response.data.email, response.data.accessToken))
                    firebaseInteractor.logSignIn()
                }
            }
            onResult(response)
        }
    }

    fun shouldSendDataToNetwork() = getToken() != null

    fun shouldSendSensorDataToNetwork(sensorId: String): Boolean {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        return shouldSendDataToNetwork() && sensorSettings?.networkSensor == true
    }

    fun shouldSendSensorDataToNetworkForOwner(sensorId: String): Boolean {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        return shouldSendDataToNetwork() && sensorSettings?.owner == getToken()?.email
    }

    suspend fun getUserInfo(): UserInfoResponse? {
        val token = getToken()
        if (token != null) {
            userInfo = networkRepository.getUserInfo(token.token)
            return userInfo
        } else {
            return null
        }
    }

    fun getSensorNetworkStatus(mac: String): SensorDataResponse? {
        return userInfo?.data?.sensors?.firstOrNull {it.sensor == mac}
    }

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

    suspend fun getSensorOwner(sensorId: String, onResult: (CheckSensorResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            try {
                val response = networkRepository.checkSensorOwner(sensorId, token)
                if (response?.isSuccess() == true && response.data?.email?.isNotEmpty() == true) {
                    sensorSettingsRepository.setSensorOwner(
                        sensorId,
                        response.data.email,
                        null
                    )
                }
                onResult(response)
            } catch (e: Exception) {
                onResult(
                    CheckSensorResponse(
                    result = RuuviNetworkResponse.errorResult,
                    error = e.message.toString(),
                    data = null,
                    code = null
                    )
                )
            }
        }
    }

    fun unclaimSensor(sensorId: String, deleteData: Boolean) {
        val networkRequest = NetworkRequest(
            NetworkRequestType.UNCLAIM,
            sensorId,
            UnclaimSensorRequest(sensorId, deleteData)
        )
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
                networkResponseLocalizer.localizeResponse(response)
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
                networkResponseLocalizer.localizeResponse(response)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun getSharedInfo(sensorId: String?, handler: CoroutineExceptionHandler, onResult: (SensorInfo?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val response = networkRepository.getSensors(sensorId, it)
                networkResponseLocalizer.localizeResponse(response)
                if (response?.isSuccess() == true && response.data != null) {
                    val result = response.data.sensors.firstOrNull { it.sensor == sensorId }
                    withContext(Dispatchers.Main) {
                        onResult(result)
                    }
                }
            }
        }
    }

    fun updateSensorCalibration(sensorId: String) {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (shouldSendSensorDataToNetworkForOwner(sensorId) && sensorSettings != null) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPDATE_SENSOR, sensorId,
                UpdateSensorRequest(
                    sensor = sensorId,
                    offsetTemperature = sensorSettings.temperatureOffset ?: 0.0,
                    offsetHumidity = sensorSettings.humidityOffset ?: 0.0,
                    offsetPressure = sensorSettings.pressureOffset ?: 0.0
                ))
            Timber.d("updateSensor $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    fun updateSensorName(sensorId: String) {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (shouldSendSensorDataToNetwork(sensorId) && sensorSettings != null) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPDATE_SENSOR, sensorId,
                UpdateSensorRequest(
                    sensor = sensorId,
                    name = sensorSettings.displayName
                ))
            Timber.d("updateSensor $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    fun updateSensorNameWithStatus(sensorId: String): Flow<OperationStatus>? {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (shouldSendSensorDataToNetwork(sensorId) && sensorSettings != null) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPDATE_SENSOR, sensorId,
                UpdateSensorRequest(
                    sensor = sensorId,
                    name = sensorSettings.displayName
                ))
            Timber.d("updateSensor $networkRequest")
            return networkRequestExecutor.registerRequestWithStatus(networkRequest)
        } else {
            return null
        }
    }

    fun uploadImage(sensorId: String, filename: String, uploadNow: Boolean = false) {
        if (shouldSendSensorDataToNetwork(sensorId)) {
            val networkRequest = NetworkRequest(NetworkRequestType.UPLOAD_IMAGE, sensorId, UploadImageRequestWrapper(filename, UploadImageRequest(sensorId)))
            Timber.d("uploadImage $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest, uploadNow)
        }
    }

    fun uploadImage(tagId: String, filename: String, handler: CoroutineExceptionHandler, onResult: (UploadImageResponse?) -> Unit) {
        val token = getToken()?.token
        CoroutineScope(Dispatchers.IO).launch(handler) {
            token?.let {
                val request = UploadImageRequest(tagId, "image/jpeg")
                val response = networkRepository.uploadImage(filename, request, token)
                networkResponseLocalizer.localizeResponse(response)
                withContext(Dispatchers.Main) {
                    onResult(response)
                }
            }
        }
    }

    fun resetImage(sensorId: String) {
        if (shouldSendSensorDataToNetwork(sensorId)) {
            val networkRequest = NetworkRequest(NetworkRequestType.RESET_IMAGE, sensorId, UploadImageRequest.getResetImageRequest(sensorId))
            Timber.d("resetImage $networkRequest")
            networkRequestExecutor.registerRequest(networkRequest)
        }
    }

    suspend fun getSensorData(request: GetSensorDataRequest):GetSensorDataResponse? = withContext(Dispatchers.IO) {
        val token = getToken()?.token
        token?.let {
            return@withContext networkRepository.getSensorData(token, request)
        }
    }

    suspend fun getSensorLastData(sensorId: String):GetSensorDataResponse? {
        val request = GetSensorDataRequest(
            sensor = sensorId,
            since = null,
            until = Date(),
            sort = SortMode.DESCENDING,
            limit = 1,
            mode = SensorDataMode.MIXED
        )
        return getSensorData(request)
    }

    suspend fun getSensorDenseLastData(
        request: SensorDenseRequest =
            SensorDenseRequest(
                sensor = null,
                measurements = true,
                alerts = false,
                sharedToOthers = false,
                sharedToMe = true,
            )
    ): SensorDenseResponse? = withContext(Dispatchers.IO) {
        val token = getToken()?.token

        token?.let {
            return@withContext networkRepository.getSensorDenseData(token, request)
        }
    }

    fun updateUserSetting(name: String, value: String) {
        val networkRequest = NetworkRequest(
            NetworkRequestType.SETTINGS,
            name,
            UpdateUserSettingRequest(name, value)
        )
        Timber.d("updateUserSetting $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest, true)
    }

    fun updateSensorSetting(sensorId: String, name: String, value: String) {
        val networkRequest = NetworkRequest(
            NetworkRequestType.SENSOR_SETTINGS,
            sensorId + name,
            UpdateSensorSettingRequest(sensorId, listOf(name), listOf(value))
        )
        Timber.d("updateSensorSetting $networkRequest")
        networkRequestExecutor.registerRequest(networkRequest, true)
    }

    fun setAlert(alarm: Alarm): Flow<OperationStatus>? {
        if (shouldSendSensorDataToNetwork(alarm.ruuviTagId) && alarm.alarmType?.networkCode != null) {
            val networkRequest = NetworkRequest(
                NetworkRequestType.SET_ALERT,
                alarm.ruuviTagId + alarm.alarmType?.networkCode,
                SetAlertRequest.getAlarmRequest(alarm)
            )
            Timber.d("setAlert $networkRequest")
            return networkRequestExecutor.registerRequestWithStatus(networkRequest)
        } else {
            return null
        }
    }

    fun requestDeleteAccount(onResult: (DeleteAccountResponse?) -> Unit) {
        val token = getToken()
        if (token != null) {
            networkRepository.requestDeleteAccount(
                email = token.email,
                token = token.token,
                onResult = onResult
            )
        }
    }

    suspend fun getSubscription(onResult: (GetSubscriptionResponse?) -> Unit) {
        val token = getToken()?.token
        token?.let {
            try {
                val response = networkRepository.getSubscription(token)
                onResult(response)
            } catch (e: Exception) {
                onResult(
                    GetSubscriptionResponse(
                        result = RuuviNetworkResponse.errorResult,
                        error = e.message.toString(),
                        data = null,
                        code = null
                    )
                )
            }
        }
    }

    suspend fun registerPush(fcmToken: String, language: String): PushRegisterResponse? {
        val token = getToken()?.token
        token?.let {
            return networkRepository.registerPush(it, fcmToken, language)
        }
        return null
    }

    suspend fun unregisterPush(fcmToken: String): PushUnregisterResponse? {
        return networkRepository.unregisterPush(fcmToken)
    }

    suspend fun getPushList(): PushListResponse? {
        val token = getToken()?.token
        token?.let {
            return networkRepository.getPushList(it)
        }
        return null
    }
}

sealed class OperationStatus {
    data object Skipped: OperationStatus()
    data object InProgress: OperationStatus()
    data object Success: OperationStatus()
    data class Fail(val reason: UiText): OperationStatus()
}