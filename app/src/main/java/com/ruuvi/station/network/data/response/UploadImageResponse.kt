package com.ruuvi.station.network.data.response

typealias UploadImageResponse = RuuviNetworkResponse<UploadImageResponseBody>

data class UploadImageResponseBody (
    val uploadURL: String,
    val guid: String
)