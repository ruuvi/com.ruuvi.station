package com.ruuvi.station.database.model

enum class NetworkRequestStatus(val code: Int) {
    READY(0),
    SUCCESS(1),
    OVERRIDDEN(2),
    PARSE_FAIL(3),
    FAILED(4);

    companion object {
        fun getById(code: Int) =
            when (code) {
                0 -> READY
                1 -> SUCCESS
                2 -> OVERRIDDEN
                3 -> PARSE_FAIL
                4 -> FAILED
                else -> throw IllegalArgumentException("Unknown NetworkRequestStatus code: $code")
            }
    }
}