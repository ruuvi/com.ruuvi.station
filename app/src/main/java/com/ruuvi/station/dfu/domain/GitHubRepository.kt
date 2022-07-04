package com.ruuvi.station.dfu.domain

import com.ruuvi.station.dfu.data.LatestReleaseResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class GitHubRepository(
    private val dispatcher: CoroutineDispatcher
) {

    private val interceptor: HttpLoggingInterceptor = HttpLoggingInterceptor().also {
        it.level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(client)
        .build()

    private val retrofitService: GitHubApi by lazy {
        retrofit.create(GitHubApi::class.java)
    }

    suspend fun getLatestFwVersion(): LatestReleaseResponse? = withContext(dispatcher){
        val response = retrofitService.getLatestRelease()
        if (response.isSuccessful) {
            return@withContext response.body()
        } else {
            Timber.d(response.errorBody().toString())
            return@withContext null
        }
    }

    suspend fun getFile(url: String) = retrofitService.getFile(url)

    companion object {
        const val BASE_URL = "https://api.github.com/repos/ruuvi/ruuvi.firmware.c/"
    }

}