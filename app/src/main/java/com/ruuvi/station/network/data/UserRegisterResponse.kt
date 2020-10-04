package com.ruuvi.station.network.data

data class UserRegisterResponse (
    val result: String,
    val data: UserRegisterDataResponse,
    val error: String,
    val tags: List<TagDataResponse>
) {
    data class UserRegisterDataResponse (
        val email: String,
        val accessToken: String
    )

    data class TagDataResponse(
        val tag: String,
        val owner: Int
    )
}
