package com.ruuvi.station.network.data.request

data class UploadImageRequest (
    val sensor: String,
    val type: String = DEFAULT_IMAGE_TYPE,
    val action: String = ACTION_UPLOAD
) {
    companion object {
        const val DEFAULT_IMAGE_TYPE = "image/jpeg"
        const val ACTION_UPLOAD = "upload"
        const val ACTION_RESET = "reset"

        fun getResetImageRequest(sensor: String) = UploadImageRequest(sensor, type = "", action = ACTION_RESET)
    }
}