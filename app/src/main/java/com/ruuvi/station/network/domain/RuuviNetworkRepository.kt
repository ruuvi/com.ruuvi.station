package com.ruuvi.station.network.domain

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.response.*
import com.ruuvi.station.util.extensions.getEpochSecond
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.Exception


class RuuviNetworkRepository
    @VisibleForTesting internal constructor(
        private val dispatcher: CoroutineDispatcher,
        private val imageInteractor: ImageInteractor
    )
{
    val ioScope = CoroutineScope(Dispatchers.IO)

    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().also {
        it.level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    private val retrofitService: RuuviNetworkApi by lazy {
        retrofit.create(RuuviNetworkApi::class.java)
    }

    fun registerUser(user: UserRegisterRequest, onResult: (UserRegisterResponse?) -> Unit) {
        ioScope.launch {
            var result: UserRegisterResponse?
            try {
                val response = retrofitService.registerUser(user)
                if (response.isSuccessful) {
                    result = response.body()
                } else {
                    val type = object : TypeToken<UserRegisterResponse>() {}.type
                    val errorResponse: UserRegisterResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                    result = errorResponse
                }
            } catch (e: Exception) {
                result = UserRegisterResponse(result = RuuviNetworkResponse.errorResult, error = e.message.orEmpty(), data = null, code = null)
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        ioScope.launch {
            var result: UserVerifyResponse?
            try {
                val response = retrofitService.verifyUser(token)
                if (response.isSuccessful) {
                    result = response.body()
                } else {
                    val type = object : TypeToken<UserVerifyResponse>() {}.type
                    val errorResponse: UserVerifyResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                    result = errorResponse
                }
            } catch (e: Exception) {
                result = UserVerifyResponse(result = RuuviNetworkResponse.errorResult, error = e.message.orEmpty(), data = null, code = null)
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    suspend fun getUserInfo(token: String): UserInfoResponse? = withContext(dispatcher){
        val response = retrofitService.getUserInfo(getAuth(token))
        val result: UserInfoResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<UserInfoResponse>() {}.type
            val errorResponse: UserInfoResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun claimSensor(request: ClaimSensorRequest, token: String, onResult: (ClaimSensorResponse?) -> Unit) {
        val response = retrofitService.claimSensor(getAuth(token), request)
        val result: ClaimSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ClaimSensorResponse>() {}.type
            val errorResponse: ClaimSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        onResult(result)
    }

    suspend fun contestSensor(request: ContestSensorRequest, token: String, onResult: (ContestSensorResponse?) -> Unit) {
        val response = retrofitService.contestSensor(getAuth(token), request)
        val result: ContestSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ContestSensorResponse>() {}.type
            val errorResponse: ContestSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        onResult(result)
    }

    suspend fun unclaimSensor(request: UnclaimSensorRequest, token: String): ClaimSensorResponse? = withContext(dispatcher) {
        val response = retrofitService.unclaimSensor(getAuth(token), request)
        val result: ClaimSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ClaimSensorResponse>() {}.type
            val errorResponse: ClaimSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun shareSensor(request: ShareSensorRequest, token: String): ShareSensorResponse? = withContext(dispatcher) {
        val response = retrofitService.shareSensor(getAuth(token), request)
        val result: ShareSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            val errorResponse: ShareSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun unshareSensor(request: UnshareSensorRequest, token: String): ShareSensorResponse? = withContext(dispatcher) {
        val response = retrofitService.unshareSensor(getAuth(token), request)
        val result: ShareSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            val errorResponse: ShareSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun getSensorData(token: String, request: GetSensorDataRequest): GetSensorDataResponse? = withContext(dispatcher) {
        val response = retrofitService.getSensorData(
            auth = getAuth(token),
            sensor = request.sensor,
            since = request.since?.getEpochSecond(),
            until = request.until?.getEpochSecond(),
            sort = request.sort?.code,
            limit = request.limit,
            mode = request.mode?.code
        )
        val result: GetSensorDataResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<GetSensorDataResponse>() {}.type
            val errorResponse: GetSensorDataResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun getSensorDenseData(token: String, request: SensorDenseRequest): SensorDenseResponse? = withContext(dispatcher) {
        val response = retrofitService.getSensorsDense(
            auth = getAuth(token),
            sensor = request.sensor,
            sharedToMe = request.sharedToMe,
            sharedToOthers = request.sharedToOthers,
            alerts = request.alerts,
            measurements = request.measurements
        )
        val result: SensorDenseResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<SensorDenseResponse>() {}.type
            val errorResponse: SensorDenseResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun updateSensor(request: UpdateSensorRequest, token: String): UpdateSensorResponse? = withContext(dispatcher) {
        Timber.d("updateSensor.request: $request")
        val response = retrofitService.updateSensor(getAuth(token), request)
        Timber.d("updateSensor.response: $response")
        val result: UpdateSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
            Timber.d("updateSensor.result: $result")
        } else {
            val type = object : TypeToken<UpdateSensorResponse>() {}.type
            val errorResponse: UpdateSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun uploadImage(filename: String, request: UploadImageRequest, token: String): UploadImageResponse? = withContext(dispatcher) {
        val response = retrofitService.uploadImage(getAuth(token), request)
        val result: UploadImageResponse?
        if (response.isSuccessful) {
            result = response.body()
            Timber.d("upload response: $result")
            result?.data?.uploadURL?.let { url->
                val fileUri = Uri.parse(filename)
                val bitmap = imageInteractor.getImage(fileUri)
                bitmap?.let {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val mediaType = request.type.toMediaType()
                    val body = stream.toByteArray().toRequestBody(mediaType)
                    retrofitService.uploadImageData(url, request.type, body)
                    bitmap.recycle()
                }
            }
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            val errorResponse: UploadImageResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun resetImage(request: UploadImageRequest, token: String): UploadImageResponse? = withContext(dispatcher) {
        val response = retrofitService.uploadImage(getAuth(token), request)
        val result: UploadImageResponse?
        if (response.isSuccessful) {
            result = response.body()
            Timber.d("reset response: $result")
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            val errorResponse: UploadImageResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    fun <T>parseError(errorBody: ResponseBody?): T? {
        return try {
            val type = object : TypeToken<T>() {}.type
            Gson().fromJson(errorBody?.charStream(), type)
        } catch (e: Exception) {
            Timber.e(e)
            null
        }
    }

    suspend fun updateUserSettings(
        request: UpdateUserSettingRequest,
        token: String
    ): UpdateUserSettingResponse? {
        val response = retrofitService.updateUserSettings(getAuth(token), request)
        return if (response.isSuccessful) {
            response.body()
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            val errorResponse: UpdateUserSettingResponse? =
                Gson().fromJson(response.errorBody()?.charStream(), type)
            errorResponse
        }
    }

    suspend fun getUserSettings(token: String): GetUserSettingsResponse? = withContext(dispatcher){
        val response = retrofitService.getUserSettings(getAuth(token))
        val result: GetUserSettingsResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<GetUserSettingsResponse>() {}.type
            val errorResponse: GetUserSettingsResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun setAlert(request: SetAlertRequest, token: String): SetAlertResponse? {
        val response = retrofitService.setAlert(getAuth(token), request)
        val result: SetAlertResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<SetAlertResponse>() {}.type
            val errorResponse: SetAlertResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        return result
    }

    suspend fun getAlerts(sensorId: String?, token: String): GetAlertsResponse? = withContext(dispatcher){
        val response = retrofitService.getAlerts(getAuth(token), sensorId)
        val result: GetAlertsResponse? = if (response.isSuccessful) {
            response.body()
        } else {
            val type = object : TypeToken<GetAlertsResponse>() {}.type
            val errorResponse: GetAlertsResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            errorResponse
        }
        result
    }

    suspend fun getSensors(sensorId: String?, token: String): GetSensorsResponse? = withContext(dispatcher){
        val response = retrofitService.getSensors(getAuth(token), sensorId)
        val result: GetSensorsResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<GetAlertsResponse>() {}.type
            val errorResponse: GetSensorsResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun checkSensorOwner(sensorId: String, token: String): CheckSensorResponse? = withContext(dispatcher){
        val response = retrofitService.checkSensorOwner(getAuth(token), sensorId)
        val result: CheckSensorResponse?
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<GetAlertsResponse>() {}.type
            val errorResponse: CheckSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    fun requestDeleteAccount(email: String, token: String, onResult: (DeleteAccountResponse?) -> Unit) {
        ioScope.launch {
            var result: DeleteAccountResponse?
            try {
                val response = retrofitService.deleteAccount(getAuth(token), DeleteAccountRequest(email))
                if (response.isSuccessful) {
                    result = response.body()
                } else {
                    val type = object : TypeToken<UserRegisterResponse>() {}.type
                    val errorResponse: DeleteAccountResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                    result = errorResponse
                }
            } catch (e: Exception) {
                result = DeleteAccountResponse(result = RuuviNetworkResponse.errorResult, error = e.message.orEmpty(), data = null, code = null)
            }

            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    suspend fun getSubscription(token: String): GetSubscriptionResponse? = withContext(dispatcher){
        val response = retrofitService.getSubscription(getAuth(token))
        val result: GetSubscriptionResponse? = if (response.isSuccessful) {
            response.body()
        } else {
            val type = object : TypeToken<GetSubscriptionResponse>() {}.type
            val errorResponse: GetSubscriptionResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            errorResponse
        }
        result
    }

    companion object {
        //private const val BASE_URL = "https://network.ruuvi.com/" //production
        private const val BASE_URL = "https://j9ul2pfmol.execute-api.eu-central-1.amazonaws.com/" //testing
        fun getAuth(token: String) = "Bearer $token"
    }
}