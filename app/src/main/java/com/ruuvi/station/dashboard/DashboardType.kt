package com.ruuvi.station.dashboard

enum class DashboardType(val code: String) {
    IMAGE_VIEW("image"),
    SIMPLE_VIEW("simple"),
    IMAGE_EXT_VIEW("image_ext");

    companion object {
        fun getByCode(code: String): DashboardType =
            DashboardType.values().firstOrNull{ it.code == code} ?: defaultDashboardType

        fun isValidCode(code: String): Boolean =
            values().any { it.code == code }

        val defaultDashboardType = IMAGE_EXT_VIEW
    }
}