package com.ruuvi.station.network.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.network.data.response.RuuviNetworkResponse

class NetworkResponseLocalizer (
    val context: Context
    ) {
    fun localizeResponse(response: RuuviNetworkResponse<*>?) {
        if (response?.isError() == true) {
            response.error = when (response.code) {
                ER_FORBIDDEN -> context.getString(R.string.cloud_er_forbidden)
                ER_UNAUTHORIZED -> context.getString(R.string.cloud_er_unauthorized)
                ER_INTERNAL -> context.getString(R.string.cloud_er_internal)
                ER_INVALID_FORMAT -> context.getString(R.string.cloud_er_invalid_format)
                ER_USER_NOT_FOUND -> context.getString(R.string.cloud_er_user_not_found)
                ER_SENSOR_NOT_FOUND -> context.getString(R.string.cloud_er_sensor_not_found)
                ER_TOKEN_EXPIRED -> context.getString(R.string.cloud_er_token_expired)
                ER_SUBSCRIPTION_NOT_FOUND -> context.getString(R.string.cloud_er_subscription_not_found)
                ER_SHARE_COUNT_REACHED -> context.getString(R.string.cloud_er_share_count_reached)
                ER_SENSOR_SHARE_COUNT_REACHED -> context.getString(R.string.cloud_er_sensor_share_count_reached)
                ER_NO_DATA_TO_SHARE -> context.getString(R.string.cloud_er_no_data_to_share)
                ER_SENSOR_ALREADY_SHARED -> context.getString(R.string.cloud_er_sensor_already_shared)
                ER_UNABLE_TO_SEND_EMAIL -> context.getString(R.string.cloud_er_unable_to_send_email)
                ER_MISSING_ARGUMENT -> context.getString(R.string.cloud_er_missing_argument)
                ER_INVALID_DENSITY_MODE -> context.getString(R.string.cloud_er_invalid_density_mode)
                ER_INVALID_SORT_MODE -> context.getString(R.string.cloud_er_invalid_sort_mode)
                ER_INVALID_TIME_RANGE -> context.getString(R.string.cloud_er_invalid_time_range)
                ER_INVALID_EMAIL_ADDRESS -> context.getString(R.string.cloud_er_invalid_email_address)
                ER_INVALID_MAC_ADDRESS -> context.getString(R.string.cloud_er_invalid_mac_address)
                ER_SUB_DATA_STORAGE_ERROR -> context.getString(R.string.cloud_er_sub_data_storage_error)
                ER_SUB_NO_USER -> context.getString(R.string.cloud_er_sub_no_user)
                else -> response.error
            }
        }
    }

    companion object {
        const val ER_FORBIDDEN = "ER_FORBIDDEN"
        const val ER_UNAUTHORIZED = "ER_UNAUTHORIZED"
        const val ER_INTERNAL = "ER_INTERNAL"
        const val ER_INVALID_FORMAT = "ER_INVALID_FORMAT"
        const val ER_USER_NOT_FOUND = "ER_USER_NOT_FOUND"
        const val ER_SENSOR_NOT_FOUND = "ER_SENSOR_NOT_FOUND"
        const val ER_TOKEN_EXPIRED = "ER_TOKEN_EXPIRED"
        const val ER_SUBSCRIPTION_NOT_FOUND = "ER_SUBSCRIPTION_NOT_FOUND"
        const val ER_SHARE_COUNT_REACHED = "ER_SHARE_COUNT_REACHED"
        const val ER_SENSOR_SHARE_COUNT_REACHED = "ER_SENSOR_SHARE_COUNT_REACHED"
        const val ER_NO_DATA_TO_SHARE = "ER_NO_DATA_TO_SHARE"
        const val ER_SENSOR_ALREADY_SHARED = "ER_SENSOR_ALREADY_SHARED"
        const val ER_UNABLE_TO_SEND_EMAIL = "ER_UNABLE_TO_SEND_EMAIL"
        const val ER_MISSING_ARGUMENT = "ER_MISSING_ARGUMENT"
        const val ER_INVALID_DENSITY_MODE = "ER_INVALID_DENSITY_MODE"
        const val ER_INVALID_SORT_MODE = "ER_INVALID_SORT_MODE"
        const val ER_INVALID_TIME_RANGE = "ER_INVALID_TIME_RANGE"
        const val ER_INVALID_EMAIL_ADDRESS = "ER_INVALID_EMAIL_ADDRESS"
        const val ER_INVALID_MAC_ADDRESS = "ER_INVALID_MAC_ADDRESS"
        const val ER_SUB_DATA_STORAGE_ERROR = "ER_SUB_DATA_STORAGE_ERROR"
        const val ER_SUB_NO_USER = "ER_SUB_NO_USER"
    }
}