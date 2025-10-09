package com.ruuvi.station.dfu.domain

import com.ruuvi.station.dfu.data.FirmwareResponse
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface FirmwareApi {
    @GET("air_firmwareupdate")
    suspend fun getFirmware(): FirmwareResponse

    @Streaming
    @GET
    suspend fun getFile(@Url url: String): ResponseBody
}