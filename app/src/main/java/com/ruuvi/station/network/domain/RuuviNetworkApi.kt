package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.request.ClaimSensorRequest
import com.ruuvi.station.network.data.request.ShareSensorRequest
import com.ruuvi.station.network.data.request.UnclaimSensorRequest
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.data.response.*
import retrofit2.Call
import retrofit2.http.*

interface RuuviNetworkApi{
    @Headers("Content-Type: application/json")
    @POST("register")
    fun registerUser(@Body request: UserRegisterRequest): Call<UserRegisterResponse>

    @Headers("Content-Type: application/json")
    @GET("verify")
    fun verifyUser(@Query("token") token: String): Call<UserVerifyResponse>

    @Headers("Content-Type: application/json")
    @GET("user")
    fun getUserInfo(@Header("Authorization") auth: String): Call<UserInfoResponse>

    @Headers("Content-Type: application/json")
    @POST("claim")
    fun claimSensor(@Header("Authorization") auth: String, @Body sensor: ClaimSensorRequest): Call<ClaimSensorResponse>

    @Headers("Content-Type: application/json")
    @POST("unclaim")
    fun unclaimSensor(@Header("Authorization") auth: String, @Body sensor: UnclaimSensorRequest): Call<ClaimSensorResponse>

    @Headers("Content-Type: application/json")
    @POST("share")
    fun shareSensor(@Header("Authorization")auth: String, @Body request: ShareSensorRequest): Call<ShareSensorResponse>

    @Headers("Content-Type: application/json")
    @GET("get")
    fun getSensorData(@Header("Authorization") auth: String, @Query("sensor") sensor: String): Call<GetSensorDataResponse>
}