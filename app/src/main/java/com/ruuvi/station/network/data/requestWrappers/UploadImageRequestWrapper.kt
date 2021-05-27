package com.ruuvi.station.network.data.requestWrappers

import com.ruuvi.station.network.data.request.UploadImageRequest

data class UploadImageRequestWrapper(
    val filename: String,
    val request: UploadImageRequest
)