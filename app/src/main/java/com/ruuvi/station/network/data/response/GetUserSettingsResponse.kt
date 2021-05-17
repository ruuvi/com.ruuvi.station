package com.ruuvi.station.network.data.response

typealias GetUserSettingsResponse = RuuviNetworkResponse<GetUserSettingsResponseBody>

data class GetUserSettingsResponseBody(val settings: NetworkUserSettings)

data class NetworkUserSettings(
    val BACKGROUND_SCAN_MODE: String?,
    val BACKGROUND_SCAN_INTERVAL: String?,
    val UNIT_TEMPERATURE: String?,
    val UNIT_HUMIDITY: String?,
    val UNIT_PRESSURE: String?,
    val DASHBOARD_ENABLED: String?,
    val CHART_SHOW_ALL_POINTS: String?,
    val CHART_DRAW_DOTS: String?,
    val CHART_VIEW_PERIOD: String?
)