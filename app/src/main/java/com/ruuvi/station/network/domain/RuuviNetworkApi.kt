package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.request.*
import com.ruuvi.station.network.data.response.*
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface RuuviNetworkApi{
    @Headers("Content-Type: application/json")
    @POST("register")
    suspend fun registerUser(@Body request: UserRegisterRequest): Response<UserRegisterResponse>

    @Headers("Content-Type: application/json")
    @GET("verify")
    suspend fun verifyUser(@Query("token") token: String): Response<UserVerifyResponse>

    @Headers("Content-Type: application/json")
    @GET("user")
    suspend fun getUserInfo(@Header("Authorization") auth: String): Response<UserInfoResponse>

    @Headers("Content-Type: application/json")
    @POST("claim")
    suspend fun claimSensor(
        @Header("Authorization") auth: String,
        @Body sensor: ClaimSensorRequest
    ): Response<ClaimSensorResponse>

    @Headers("Content-Type: application/json")
    @POST("unclaim")
    suspend fun unclaimSensor(
        @Header("Authorization") auth: String,
        @Body sensor: UnclaimSensorRequest
    ): Response<ClaimSensorResponse>

    @Headers("Content-Type: application/json")
    @POST("share")
    suspend fun shareSensor(
        @Header("Authorization")auth: String,
        @Body request: ShareSensorRequest
    ): Response<ShareSensorResponse>

    @Headers("Content-Type: application/json")
    @POST("unshare")
    suspend fun unshareSensor(
        @Header("Authorization")auth: String,
        @Body request: UnshareSensorRequest
    ): Response<ShareSensorResponse>

    @Headers("Content-Type: application/json")
    @GET("shared")
    suspend fun getSharedSensors(@Header("Authorization") auth: String): Response<SharedSensorsResponse>

    @Headers("Content-Type: application/json")
    @GET("get")
    suspend fun getSensorData(
        @Header("Authorization") auth: String,
        @Query("sensor") sensor: String,
        @Query("since") since: Long?,
        @Query("until") until: Long?,
        @Query("sort") sort: String?,
        @Query("limit") limit: Int?
    ): Response<GetSensorDataResponse>

    @Headers("Content-Type: application/json")
    @POST("update")
    suspend fun updateSensor(
        @Header("Authorization") auth: String,
        @Body request: UpdateSensorRequest
    ): Response<UpdateSensorResponse>

    @Headers("Content-Type: application/json")
    @POST("upload")
    suspend fun uploadImage(
        @Header("Authorization") auth: String,
        @Body request: UploadImageRequest
    ): Response<UploadImageResponse>

    @PUT
    suspend fun uploadImageData(
        @Url url: String,
        @Header("Content-Type") type: String,
        @Body data: RequestBody
    )
}