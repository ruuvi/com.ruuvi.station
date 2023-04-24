package com.ruuvi.station.dashboard

enum class DashboardTapAction(val code: String) {
    OPEN_CARD("card"),
    SHOW_CHART("chart");

    companion object {
        fun getByCode(code: String): DashboardTapAction =
            DashboardTapAction.values().firstOrNull{ it.code == code} ?: defaultDashboardTapAction

        fun isValidCode(code: String): Boolean =
            DashboardTapAction.values().any { it.code == code }

        val defaultDashboardTapAction = OPEN_CARD
    }
}