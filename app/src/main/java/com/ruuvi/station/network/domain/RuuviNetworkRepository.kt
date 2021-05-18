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
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.lang.Exception


class RuuviNetworkRepository
    @VisibleForTesting internal constructor(
        val dispatcher: CoroutineDispatcher,
        val imageInteractor: ImageInteractor
    )
{
    val ioScope = CoroutineScope(Dispatchers.IO)

    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().also {
        it.level = HttpLoggingInterceptor.Level.BODY;
    }

    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    val retrofitService: RuuviNetworkApi by lazy {
        retrofit.create(RuuviNetworkApi::class.java)
    }

    fun registerUser(user: UserRegisterRequest, onResult: (UserRegisterResponse?) -> Unit) {
        ioScope.launch {
            val response = retrofitService.registerUser(user)
            var result: UserRegisterResponse? = null
            if (response.isSuccessful) {
                result = response.body()
            } else {
                val type = object : TypeToken<UserRegisterResponse>() {}.type
                var errorResponse: UserRegisterResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                result = errorResponse
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        ioScope.launch {
            val response = retrofitService.verifyUser(token)
            var result: UserVerifyResponse? = null
            if (response.isSuccessful) {
                result = response.body()
            } else {
                val type = object : TypeToken<UserVerifyResponse>() {}.type
                var errorResponse: UserVerifyResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                result = errorResponse
            }
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    suspend fun getUserInfo(token: String): UserInfoResponse? = withContext(dispatcher){
        val response = retrofitService.getUserInfo(getAuth(token))
        var result: UserInfoResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<UserInfoResponse>() {}.type
            var errorResponse: UserInfoResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun claimSensor(request: ClaimSensorRequest, token: String, onResult: (ClaimSensorResponse?) -> Unit) {
        val response = retrofitService.claimSensor(getAuth(token), request)
        var result: ClaimSensorResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ClaimSensorResponse>() {}.type
            var errorResponse: ClaimSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        onResult(result)
    }

    suspend fun unclaimSensor(request: UnclaimSensorRequest, token: String): ClaimSensorResponse? = withContext(dispatcher) {
        val response = retrofitService.unclaimSensor(getAuth(token), request)
        var result: ClaimSensorResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ClaimSensorResponse>() {}.type
            var errorResponse: ClaimSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun shareSensor(request: ShareSensorRequest, token: String): ShareSensorResponse? = withContext(dispatcher) {
        val response = retrofitService.shareSensor(getAuth(token), request)
        var result: ShareSensorResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            var errorResponse: ShareSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun unshareSensor(request: UnshareSensorRequest, token: String): ShareSensorResponse? = withContext(dispatcher) {
        val response = retrofitService.unshareSensor(getAuth(token), request)
        var result: ShareSensorResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            var errorResponse: ShareSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun getSharedSensors(token: String): SharedSensorsResponse? = withContext(dispatcher){
        val response = retrofitService.getSharedSensors(getAuth(token))
        var result: SharedSensorsResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<SharedSensorsResponse>() {}.type
            var errorResponse: SharedSensorsResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun getSensorData(token: String, request: GetSensorDataRequest): GetSensorDataResponse? = withContext(dispatcher) {
        val response = retrofitService.getSensorData(
            getAuth(token),
            request.sensor,
            request.since?.getEpochSecond(),
            request.until?.getEpochSecond(),
            request.sort,
            request.limit
        )
        var result: GetSensorDataResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<GetSensorDataResponse>() {}.type
            var errorResponse: GetSensorDataResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun updateSensor(request: UpdateSensorRequest, token: String): UpdateSensorResponse? = withContext(dispatcher) {
        Timber.d("updateSensor.request: $request")
        val response = retrofitService.updateSensor(getAuth(token), request)
        Timber.d("updateSensor.response: $response")
        var result: UpdateSensorResponse? = null
        if (response.isSuccessful) {
            result = response.body()
            Timber.d("updateSensor.result: $result")
        } else {
            val type = object : TypeToken<UpdateSensorResponse>() {}.type
            var errorResponse: UpdateSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun uploadImage(filename: String, request: UploadImageRequest, token: String): UploadImageResponse? = withContext(dispatcher) {
        val response = retrofitService.uploadImage(getAuth(token), request)
        var result: UploadImageResponse? = null
        if (response.isSuccessful) {
            result = response.body()
            Timber.d("upload response: $result")
            result?.data?.uploadURL?.let { url->
                val fileUri = Uri.parse(filename)
                val bitmap = imageInteractor.getImage(fileUri)
                bitmap?.let {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    val mediaType = MediaType.get(request.type)
                    val body = RequestBody.create(mediaType, stream.toByteArray())
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
        var result: UploadImageResponse? = null
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
        try {
            val type = object : TypeToken<T>() {}.type
            var errorResponse: T? = Gson().fromJson(errorBody?.charStream(), type)
            return errorResponse
        } catch (e: Exception) {
            Timber.e(e)
            return null
        }
    }

    suspend fun updateUserSettings(request: UpdateUserSettingRequest, token: String) {
        val response = retrofitService.updateUserSettings(getAuth(token), request)
        var result: UpdateUserSettingResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<ShareSensorResponse>() {}.type
            var errorResponse: UpdateUserSettingResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    suspend fun getUserSettings(token: String): GetUserSettingsResponse? = withContext(dispatcher){
        val response = retrofitService.getUserSettings(getAuth(token))
        var result: GetUserSettingsResponse? = null
        if (response.isSuccessful) {
            result = response.body()
        } else {
            val type = object : TypeToken<GetUserSettingsResponse>() {}.type
            var errorResponse: GetUserSettingsResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
            result = errorResponse
        }
        result
    }

    companion object {
        private const val BASE_URL = "https://network.ruuvi.com/"

        fun getAuth(token: String) = "Bearer $token"
    }
}