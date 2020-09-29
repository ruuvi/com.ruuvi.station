package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.UserRegisterRequest
import com.ruuvi.station.network.data.UserRegisterResponse
import com.ruuvi.station.network.data.UserVerifyRequest
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*

interface RuuviNetworkApi{
    @Headers("Content-Type: application/json")
    @POST("register")
    fun registerUser(@Body request: UserRegisterRequest): Call<UserRegisterResponse>

    @Headers("Content-Type: application/json")
    @GET("verify")
    fun verifyUser(@Query("token") request: UserVerifyRequest): Call<UserRegisterResponse>
}