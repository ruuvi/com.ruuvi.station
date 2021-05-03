package com.ruuvi.station.database.model

enum class NetworkRequestType(val code: Int) {
    UNCLAIM(1),
    UPDATE_SENSOR(2),
    UPDATE_IMAGE(3),
    SETTING(4),
    UNSHARE(5);

    companion object {
        fun getById(code: Int) =
            when (code) {
                1 -> UNCLAIM
                2 -> UPDATE_SENSOR
                3 -> UPDATE_IMAGE
                4 -> SETTING
                5 -> UNSHARE
                else -> throw IllegalArgumentException("Unknown NetworkRequestType code: $code")
            }
    }
}