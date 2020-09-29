package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.UserRegisterRequest
import com.ruuvi.station.network.data.UserRegisterResponse
import retrofit2.Call
import retrofit2.http.*

interface RuuviNetworkApi{
    @Headers("Content-Type: application/json")
    @POST("register")
    @FormUrlEncoded
    fun registerUser(@Field("reset") reset: Int = 0,
                     @Field("email") email: String): Call<UserRegisterResponse>
}