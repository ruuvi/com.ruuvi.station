package com.ruuvi.station.settings.domain

enum class ResolutionSettingsTarget(val routeCode: String) {
    Temperature("temperature"),
    RelativeHumidity("relative_humidity"),
    AbsoluteHumidity("absolute_humidity"),
    DewPoint("dew_point"),
    Pressure("pressure"),
    ParticulateMatter("particulate_matter"),
    Acceleration("acceleration"),
    Voltage("voltage");

    companion object {
        fun getByRouteCode(routeCode: String?): ResolutionSettingsTarget? =
            values().firstOrNull { it.routeCode == routeCode }
    }
}
