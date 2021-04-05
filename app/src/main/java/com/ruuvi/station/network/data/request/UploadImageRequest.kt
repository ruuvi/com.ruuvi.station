package com.ruuvi.station.network.data.request

data class UploadImageRequest (
    val sensor: String,
    val type: String,
    val action: String = "upload"
) {
    companion object {
        fun getResetImageRequest(sensor: String) = UploadImageRequest(sensor, type = "", action = "reset")
    }
}