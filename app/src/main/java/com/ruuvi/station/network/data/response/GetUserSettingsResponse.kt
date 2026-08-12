package com.ruuvi.station.network.data.response

import com.google.gson.annotations.SerializedName

typealias GetUserSettingsResponse = RuuviNetworkResponse<GetUserSettingsResponseBody>

data class GetUserSettingsResponseBody(val settings: NetworkUserSettings)

data class NetworkUserSettings(
    val BACKGROUND_SCAN_MODE: String?,
    val BACKGROUND_SCAN_MODE_lastUpdated: Long?,
    val BACKGROUND_SCAN_INTERVAL: String?,
    val BACKGROUND_SCAN_INTERVAL_lastUpdated: Long?,
    val UNIT_TEMPERATURE: String?,
    val UNIT_TEMPERATURE_lastUpdated: Long?,
    val UNIT_HUMIDITY: String?,
    val UNIT_HUMIDITY_lastUpdated: Long?,
    val UNIT_PRESSURE: String?,
    val UNIT_PRESSURE_lastUpdated: Long?,
    val ACCURACY_TEMPERATURE: String?,
    val ACCURACY_TEMPERATURE_lastUpdated: Long?,
    val ACCURACY_HUMIDITY: String?,
    val ACCURACY_HUMIDITY_lastUpdated: Long?,
    val ACCURACY_HUMIDITY_RELATIVE: String?,
    val ACCURACY_HUMIDITY_RELATIVE_lastUpdated: Long?,
    val ACCURACY_HUMIDITY_ABSOLUTE: String?,
    val ACCURACY_HUMIDITY_ABSOLUTE_lastUpdated: Long?,
    val ACCURACY_HUMIDITY_DEW_POINT: String?,
    val ACCURACY_HUMIDITY_DEW_POINT_lastUpdated: Long?,
    val ACCURACY_PRESSURE: String?,
    val ACCURACY_PRESSURE_lastUpdated: Long?,
    val ACCURACY_PM: String?,
    val ACCURACY_PM_lastUpdated: Long?,
    val ACCURACY_ACCELERATION: String?,
    val ACCURACY_ACCELERATION_lastUpdated: Long?,
    val ACCURACY_VOLTAGE: String?,
    val ACCURACY_VOLTAGE_lastUpdated: Long?,
    val CLOUD_MODE_ENABLED: String?,
    val CLOUD_MODE_ENABLED_lastUpdated: Long?,
    val CHART_SHOW_ALL_POINTS: String?,
    val CHART_SHOW_ALL_POINTS_lastUpdated: Long?,
    val CHART_DRAW_DOTS: String?,
    val CHART_DRAW_DOTS_lastUpdated: Long?,
    val DASHBOARD_TYPE: String?,
    val DASHBOARD_TYPE_lastUpdated: Long?,
    val DASHBOARD_TAP_ACTION: String?,
    val DASHBOARD_TAP_ACTION_lastUpdated: Long?,
    val PROFILE_LANGUAGE_CODE: String?,
    val PROFILE_LANGUAGE_CODE_lastUpdated: Long?,
    val SENSOR_ORDER: String?,
    val SENSOR_ORDER_lastUpdated: Long?,
    val DISABLE_EMAIL_NOTIFICATIONS: String?,
    val DISABLE_EMAIL_NOTIFICATIONS_lastUpdated: Long?,
    val DISABLE_PUSH_NOTIFICATIONS: String?,
    val DISABLE_PUSH_NOTIFICATIONS_lastUpdated: Long?,
    val DISABLE_TELEGRAM_NOTIFICATIONS: String?,
    val DISABLE_TELEGRAM_NOTIFICATIONS_lastUpdated: Long?,
    val TIPS_ALLOWED: String?,
    val TIPS_ALLOWED_lastUpdated: Long?,
    @SerializedName(value = "MARKTING_PERMISSION")
    val MARKETING_PERMISSION: String?,
    @SerializedName(value = "MARKTING_PERMISSION_lastUpdated")
    val MARKETING_PERMISSION_lastUpdated: Long?,
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
                MARKETING_PERMISSION == null &&
                PROFILE_LANGUAGE_CODE == null &&
                TIPS_ALLOWED == null

    fun valueFor(settingName: String): String? {
        return when (settingName) {
            "BACKGROUND_SCAN_MODE" -> BACKGROUND_SCAN_MODE
            "BACKGROUND_SCAN_INTERVAL" -> BACKGROUND_SCAN_INTERVAL
            "UNIT_TEMPERATURE" -> UNIT_TEMPERATURE
            "UNIT_HUMIDITY" -> UNIT_HUMIDITY
            "UNIT_PRESSURE" -> UNIT_PRESSURE
            "ACCURACY_TEMPERATURE" -> ACCURACY_TEMPERATURE
            "ACCURACY_HUMIDITY" -> ACCURACY_HUMIDITY
            "ACCURACY_HUMIDITY_RELATIVE" -> ACCURACY_HUMIDITY_RELATIVE
            "ACCURACY_HUMIDITY_ABSOLUTE" -> ACCURACY_HUMIDITY_ABSOLUTE
            "ACCURACY_HUMIDITY_DEW_POINT" -> ACCURACY_HUMIDITY_DEW_POINT
            "ACCURACY_PRESSURE" -> ACCURACY_PRESSURE
            "ACCURACY_PM" -> ACCURACY_PM
            "ACCURACY_ACCELERATION" -> ACCURACY_ACCELERATION
            "ACCURACY_VOLTAGE" -> ACCURACY_VOLTAGE
            "CLOUD_MODE_ENABLED" -> CLOUD_MODE_ENABLED
            "CHART_SHOW_ALL_POINTS" -> CHART_SHOW_ALL_POINTS
            "CHART_DRAW_DOTS" -> CHART_DRAW_DOTS
            "DASHBOARD_TYPE" -> DASHBOARD_TYPE
            "DASHBOARD_TAP_ACTION" -> DASHBOARD_TAP_ACTION
            "PROFILE_LANGUAGE_CODE" -> PROFILE_LANGUAGE_CODE
            "SENSOR_ORDER" -> SENSOR_ORDER
            "DISABLE_EMAIL_NOTIFICATIONS" -> DISABLE_EMAIL_NOTIFICATIONS
            "DISABLE_PUSH_NOTIFICATIONS" -> DISABLE_PUSH_NOTIFICATIONS
            "DISABLE_TELEGRAM_NOTIFICATIONS" -> DISABLE_TELEGRAM_NOTIFICATIONS
            "TIPS_ALLOWED" -> TIPS_ALLOWED
            "MARKTING_PERMISSION" -> MARKETING_PERMISSION
            else -> null
        }
    }

    fun timestampFor(settingName: String): Long {
        return when (settingName) {
            "BACKGROUND_SCAN_MODE" -> BACKGROUND_SCAN_MODE_lastUpdated
            "BACKGROUND_SCAN_INTERVAL" -> BACKGROUND_SCAN_INTERVAL_lastUpdated
            "UNIT_TEMPERATURE" -> UNIT_TEMPERATURE_lastUpdated
            "UNIT_HUMIDITY" -> UNIT_HUMIDITY_lastUpdated
            "UNIT_PRESSURE" -> UNIT_PRESSURE_lastUpdated
            "ACCURACY_TEMPERATURE" -> ACCURACY_TEMPERATURE_lastUpdated
            "ACCURACY_HUMIDITY" -> ACCURACY_HUMIDITY_lastUpdated
            "ACCURACY_HUMIDITY_RELATIVE" -> ACCURACY_HUMIDITY_RELATIVE_lastUpdated
            "ACCURACY_HUMIDITY_ABSOLUTE" -> ACCURACY_HUMIDITY_ABSOLUTE_lastUpdated
            "ACCURACY_HUMIDITY_DEW_POINT" -> ACCURACY_HUMIDITY_DEW_POINT_lastUpdated
            "ACCURACY_PRESSURE" -> ACCURACY_PRESSURE_lastUpdated
            "ACCURACY_PM" -> ACCURACY_PM_lastUpdated
            "ACCURACY_ACCELERATION" -> ACCURACY_ACCELERATION_lastUpdated
            "ACCURACY_VOLTAGE" -> ACCURACY_VOLTAGE_lastUpdated
            "CLOUD_MODE_ENABLED" -> CLOUD_MODE_ENABLED_lastUpdated
            "CHART_SHOW_ALL_POINTS" -> CHART_SHOW_ALL_POINTS_lastUpdated
            "CHART_DRAW_DOTS" -> CHART_DRAW_DOTS_lastUpdated
            "DASHBOARD_TYPE" -> DASHBOARD_TYPE_lastUpdated
            "DASHBOARD_TAP_ACTION" -> DASHBOARD_TAP_ACTION_lastUpdated
            "PROFILE_LANGUAGE_CODE" -> PROFILE_LANGUAGE_CODE_lastUpdated
            "SENSOR_ORDER" -> SENSOR_ORDER_lastUpdated
            "DISABLE_EMAIL_NOTIFICATIONS" -> DISABLE_EMAIL_NOTIFICATIONS_lastUpdated
            "DISABLE_PUSH_NOTIFICATIONS" -> DISABLE_PUSH_NOTIFICATIONS_lastUpdated
            "DISABLE_TELEGRAM_NOTIFICATIONS" -> DISABLE_TELEGRAM_NOTIFICATIONS_lastUpdated
            "TIPS_ALLOWED" -> TIPS_ALLOWED_lastUpdated
            "MARKTING_PERMISSION" -> MARKETING_PERMISSION_lastUpdated
            else -> null
        } ?: 0L
    }
}
