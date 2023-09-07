package com.ruuvi.station.network.data.request

import android.os.Build

data class PushRegisterRequest(
    val token: String,
    val type: String = "Android",
    val name: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val params: PushRegisterParams
)

data class PushRegisterParams(
    val language: String
)