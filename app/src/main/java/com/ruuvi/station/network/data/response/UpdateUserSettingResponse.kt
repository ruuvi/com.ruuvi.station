package com.ruuvi.station.network.data.response

typealias UpdateUserSettingResponse = RuuviNetworkResponse<UpdateUserSettingResponseBody>

data class UpdateUserSettingResponseBody(
    val action: String
)