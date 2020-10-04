package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.ClaimTagRequest
import com.ruuvi.station.network.data.UserRegisterRequest
import com.ruuvi.station.network.data.UserRegisterResponse
import retrofit2.Call
import retrofit2.http.*

interface RuuviNetworkApi{
    @Headers("Content-Type: application/json")
    @POST("register")
    fun registerUser(@Body request: UserRegisterRequest): Call<UserRegisterResponse>

    @Headers("Content-Type: application/json")
    @GET("verify")
    fun verifyUser(@Query("token") token: String): Call<UserRegisterResponse>

    @Headers("Content-Type: application/json")
    @GET("user")
    fun getUserInfo(@Header("Authorization") auth: String): Call<UserRegisterResponse>

    @Headers("Content-Type: application/json")
    @POST("claim")
    fun claimTag(@Header("Authorization") auth: String, @Body tag: ClaimTagRequest): Call<UserRegisterResponse>
}