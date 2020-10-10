package com.ruuvi.station.network.domain

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ShareSensorRequest
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.data.response.*
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.lang.Exception


class RuuviNetworkRepository {

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
        retrofitService.registerUser(user).enqueue(
            object : Callback<UserRegisterResponse> {
                override fun onFailure(call: Call<UserRegisterResponse>, t: Throwable) {
                    println(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<UserRegisterResponse>, response: Response<UserRegisterResponse>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        val type = object : TypeToken<UserRegisterResponse>() {}.type
                        var errorResponse: UserRegisterResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                        onResult(errorResponse)
                    }
                }
            }
        )
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        retrofitService.verifyUser(token).enqueue(
            object : Callback<UserVerifyResponse> {
                override fun onFailure(call: Call<UserVerifyResponse>, t: Throwable) {
                    Timber.e(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<UserVerifyResponse>, response: Response<UserVerifyResponse>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        val type = object : TypeToken<UserVerifyResponse>() {}.type
                        var errorResponse: UserVerifyResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                        onResult(errorResponse)
                    }
                }
            }
        )
    }



    fun getUserInfo(token: String, onResult: (UserInfoResponse?) -> Unit) {
        retrofitService.getUserInfo("Bearer " + token).enqueue(
            object : Callback<UserInfoResponse> {
                override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                    Timber.e(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<UserInfoResponse>, response: Response<UserInfoResponse>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        val type = object : TypeToken<UserInfoResponse>() {}.type
                        var errorResponse: UserInfoResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                        onResult(errorResponse)
                    }
                }
            }
        )
    }

    fun claimSensor(request: ClaimSensorRequest, token: String, onResult: (ClaimSensorResponse?) -> Unit) {
        retrofitService.claimSensor("Bearer " + token, request).enqueue(
            object : Callback<ClaimSensorResponse> {
                override fun onFailure(call: Call<ClaimSensorResponse>, t: Throwable) {
                    Timber.e(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<ClaimSensorResponse>, response: Response<ClaimSensorResponse>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        val type = object : TypeToken<ClaimSensorResponse>() {}.type
                        var errorResponse: ClaimSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                        onResult(errorResponse)
                    }
                }
            }
        )
    }

    fun shareSensor(request: ShareSensorRequest, token: String, onResult: (ShareSensorResponse?) -> Unit) {
        retrofitService.shareSensor("Bearer " + token, request).enqueue(
            object : Callback<ShareSensorResponse> {
                override fun onFailure(call: Call<ShareSensorResponse>, t: Throwable) {
                    Timber.e(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<ShareSensorResponse>, response: Response<ShareSensorResponse>) {
                    if (response.isSuccessful) {
                        onResult(response.body())
                    } else {
                        val type = object : TypeToken<ShareSensorResponse>() {}.type
                        var errorResponse: ShareSensorResponse? = Gson().fromJson(response.errorBody()?.charStream(), type)
                        onResult(errorResponse)
                    }
                }
            }
        )
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

    companion object {
        private const val BASE_URL = "https://network.ruuvi.com/"
    }
}