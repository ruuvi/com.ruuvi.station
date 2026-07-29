package com.ruuvi.station.settings.domain

enum class GlobalUnitsSettingsType(val routeCode: String) {
    Temperature("temperature"),
    Humidity("humidity"),
    Pressure("pressure");

    companion object {
        fun getByRouteCode(routeCode: String?): GlobalUnitsSettingsType? =
            values().firstOrNull { it.routeCode == routeCode }
    }
}
