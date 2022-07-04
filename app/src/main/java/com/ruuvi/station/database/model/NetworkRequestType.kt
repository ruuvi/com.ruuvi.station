package com.ruuvi.station.database.model

enum class NetworkRequestType(val code: Int) {
    UNCLAIM(1),
    UPDATE_SENSOR(2),
    UPLOAD_IMAGE(3),
    SETTINGS(4),
    UNSHARE(5),
    RESET_IMAGE(6),
    SET_ALERT(7);

    companion object {
        fun getById(code: Int) =
            when (code) {
                1 -> UNCLAIM
                2 -> UPDATE_SENSOR
                3 -> UPLOAD_IMAGE
                4 -> SETTINGS
                5 -> UNSHARE
                6 -> RESET_IMAGE
                7 -> SET_ALERT
                else -> throw IllegalArgumentException("Unknown NetworkRequestType code: $code")
            }
    }
}