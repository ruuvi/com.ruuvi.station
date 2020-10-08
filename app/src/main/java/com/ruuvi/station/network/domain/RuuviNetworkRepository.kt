package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ShareSensorRequest
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.data.response.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
                    val registeredUser = response.body()
                    onResult(registeredUser)
                }
            }
        )
    }

    fun verifyUser(token: String, onResult: (UserVerifyResponse?) -> Unit) {
        retrofitService.verifyUser(token).enqueue(
            object : Callback<UserVerifyResponse> {
                override fun onFailure(call: Call<UserVerifyResponse>, t: Throwable) {
                    println(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<UserVerifyResponse>, response: Response<UserVerifyResponse>) {
                    val registeredUser = response.body()
                    onResult(registeredUser)
                }
            }
        )
    }

    fun getUserInfo(token: String, onResult: (UserInfoResponse?) -> Unit) {
        retrofitService.getUserInfo("Bearer " + token).enqueue(
            object : Callback<UserInfoResponse> {
                override fun onFailure(call: Call<UserInfoResponse>, t: Throwable) {
                    println(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<UserInfoResponse>, response: Response<UserInfoResponse>) {
                    val registeredUser = response.body()
                    onResult(registeredUser)
                }
            }
        )
    }

    fun claimSensor(request: ClaimSensorRequest, token: String, onResult: (ClaimSensorResponse?) -> Unit) {
        retrofitService.claimSensor("Bearer " + token, request).enqueue(
            object : Callback<ClaimSensorResponse> {
                override fun onFailure(call: Call<ClaimSensorResponse>, t: Throwable) {
                    println(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<ClaimSensorResponse>, response: Response<ClaimSensorResponse>) {
                    val registeredUser = response.body()
                    onResult(registeredUser)
                }
            }
        )
    }

    fun shareSensor(request: ShareSensorRequest, token: String, onResult: (ShareSensorResponse?) -> Unit) {
        retrofitService.shareSensor("Bearer " + token, request).enqueue(
            object : Callback<ShareSensorResponse> {
                override fun onFailure(call: Call<ShareSensorResponse>, t: Throwable) {
                    println(t)
                    onResult(null)
                }

                override fun onResponse(call: Call<ShareSensorResponse>, response: Response<ShareSensorResponse>) {
                    val registeredUser = response.body()
                    onResult(registeredUser)
                }
            }
        )
    }

    companion object {
        private const val BASE_URL = "https://network.ruuvi.com/"
    }
}