package com.ruuvi.station.dfu.domain

import com.ruuvi.station.dfu.data.FirmwareInfo
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

class FirmwareRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://testnet.ruuvi.com/") // base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(FirmwareApi::class.java)

    suspend fun getLatest(): FirmwareInfo? {
        try {
            val response = api.getFirmware()
            if (response.result == "success") {
                val latestVersion = response.data.latest
                Timber.d("Latest: ${latestVersion.version} at ${latestVersion.url}")
                return latestVersion
            }
        } catch (e: Exception) {
            Timber.e("Error fetching firmware: ${e.message}")
        }
        return null
    }

    suspend fun getFile(url: String) = api.getFile(url)
}