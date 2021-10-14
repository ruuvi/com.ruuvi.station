package com.ruuvi.station.dfu.domain

import com.ruuvi.station.dfu.data.LatestReleaseResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Streaming
import retrofit2.http.Url

interface GitHubApi {
    @Headers("Content-Type: application/json")
    @GET("releases/latest")
    suspend fun getLatestRelease(): Response<LatestReleaseResponse>

    @Streaming
    @GET
    suspend fun getFile(@Url url: String): ResponseBody
}