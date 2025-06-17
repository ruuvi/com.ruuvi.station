package com.ruuvi.station.network.data.response

typealias UpdateSensorSettingResponse = RuuviNetworkResponse<UpdateSensorSettingResponseBody>

data class UpdateSensorSettingResponseBody(
    val action: String
)