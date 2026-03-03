package com.ruuvi.station.dfu.domain

import com.ruuvi.station.dfu.data.FirmwareInfo
import com.ruuvi.station.dfu.ui.FirmwareVersionOption
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import kotlin.String

class FirmwareRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://network.ruuvi.com/") // base URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(FirmwareApi::class.java)

    suspend fun getLatest(): FirmwareInfo? {
        try {
            val response = api.getFirmware()
            if (response.result == "success") {
                val latestVersion = response.data.latest
                Timber.d("Latest: ${latestVersion?.version} at ${latestVersion?.url}")
                return latestVersion
            }
        } catch (e: Exception) {
            Timber.e("Error fetching firmware: ${e.message}")
        }
        return null
    }

    suspend fun getOptions(): List<FirmwareVersionOption>? {
        try {
            val response = api.getFirmware()
            if (response.result == "success") {
                val releases = mutableListOf<FirmwareVersionOption>()

                val latestVersion = response.data.latest?.version
                response.data.latest?.let {
                    releases.add(
                        FirmwareVersionOption.Latest(
                            it.version,
                            it.url,
                            it.created_at,
                            it.versionCode,
                            it.fileName,
                            it.fwloader,
                            it.mcuboot_s1,
                            it.mcuboot
                        ))
                }

                response.data.beta?.let {
                    if (latestVersion != it.version) {
                        releases.add(
                            FirmwareVersionOption.Beta(
                                it.version,
                                it.url,
                                it.created_at,
                                it.versionCode,
                                it.fileName,
                                it.fwloader,
                                it.mcuboot_s1,
                                it.mcuboot
                            ))
                    }
                }

                response.data.alpha?.let {
                    if (latestVersion != it.version) {
                        releases.add(
                            FirmwareVersionOption.Alpha(
                                it.version,
                                it.url,
                                it.created_at,
                                it.versionCode,
                                it.fileName,
                                it.fwloader,
                                it.mcuboot_s1,
                                it.mcuboot
                            )
                        )
                    }
                }

                return releases
            }
        } catch (e: Exception) {
            Timber.e("Error fetching firmware: ${e.message}")
        }
        return null
    }

    suspend fun getFile(url: String) = api.getFile(url)
}