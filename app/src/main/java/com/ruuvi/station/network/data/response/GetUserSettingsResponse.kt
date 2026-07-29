package com.ruuvi.station.network.data.response

typealias GetUserSettingsResponse = RuuviNetworkResponse<GetUserSettingsResponseBody>

data class GetUserSettingsResponseBody(val settings: NetworkUserSettings)

data class NetworkUserSettings(
    val BACKGROUND_SCAN_MODE: String?,
    val BACKGROUND_SCAN_INTERVAL: String?,
    val UNIT_TEMPERATURE: String?,
    val UNIT_HUMIDITY: String?,
    val UNIT_PRESSURE: String?,
    val ACCURACY_TEMPERATURE: String?,
    val ACCURACY_HUMIDITY: String?,
    val ACCURACY_HUMIDITY_RELATIVE: String?,
    val ACCURACY_HUMIDITY_ABSOLUTE: String?,
    val ACCURACY_HUMIDITY_DEW_POINT: String?,
    val ACCURACY_PRESSURE: String?,
    val ACCURACY_PM: String?,
    val ACCURACY_ACCELERATION: String?,
    val ACCURACY_VOLTAGE: String?,
    val CLOUD_MODE_ENABLED: String?,
    val CHART_SHOW_ALL_POINTS: String?,
    val CHART_DRAW_DOTS: String?,
    val DASHBOARD_TYPE: String?,
    val DASHBOARD_TAP_ACTION: String?,
    val PROFILE_LANGUAGE_CODE: String?,
    val SENSOR_ORDER: String?,
    val DISABLE_EMAIL_NOTIFICATIONS: String?,
    val DISABLE_PUSH_NOTIFICATIONS: String?,
    val DISABLE_TELEGRAM_NOTIFICATIONS: String?,
    val MARKTING_PERMISSION: String?,
) {
    fun isEmpty() =
                BACKGROUND_SCAN_MODE == null &&
                BACKGROUND_SCAN_INTERVAL == null &&
                UNIT_TEMPERATURE == null &&
                UNIT_HUMIDITY == null &&
                UNIT_PRESSURE == null &&
                ACCURACY_TEMPERATURE == null &&
                ACCURACY_HUMIDITY == null &&
                ACCURACY_HUMIDITY_RELATIVE == null &&
                ACCURACY_HUMIDITY_ABSOLUTE == null &&
                ACCURACY_HUMIDITY_DEW_POINT == null &&
                ACCURACY_PRESSURE == null &&
                ACCURACY_PM == null &&
                ACCURACY_ACCELERATION == null &&
                ACCURACY_VOLTAGE == null &&
                DASHBOARD_TYPE == null &&
                DASHBOARD_TAP_ACTION == null &&
                CLOUD_MODE_ENABLED == null &&
                CHART_SHOW_ALL_POINTS == null &&
                CHART_DRAW_DOTS == null &&
                SENSOR_ORDER == null &&
                DISABLE_EMAIL_NOTIFICATIONS == null &&
                DISABLE_PUSH_NOTIFICATIONS == null &&
                DISABLE_TELEGRAM_NOTIFICATIONS == null &&
                MARKTING_PERMISSION == null
}
