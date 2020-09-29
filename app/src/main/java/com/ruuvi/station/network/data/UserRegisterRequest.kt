package com.ruuvi.station.network.data

data class UserRegisterRequest (
    val reset: Int = 0,
    val email: String
)