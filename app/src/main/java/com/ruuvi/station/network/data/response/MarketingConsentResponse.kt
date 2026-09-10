package com.ruuvi.station.network.data.response

typealias MarketingConsentResponse = RuuviNetworkResponse<MarketingConsentResponseBody>

data class MarketingConsentResponseBody(
    val consent: Boolean,
    val status: String?
) {
    val consentStatus: MarketingConsentStatus
        get() = MarketingConsentStatus.fromValue(status)
}

enum class MarketingConsentStatus(val value: String) {
    SUBSCRIBED("subscribed"),
    UNSUBSCRIBED("unsubscribed"),
    UNCONFIRMED("unconfirmed"),
    BOUNCED("bounced"),
    SOFT_BOUNCED("soft_bounced"),
    COMPLAINED("complained"),
    NOT_FOUND("not_found"),
    UNKNOWN("unknown");

    val isToggleEnabled: Boolean
        get() = this == SUBSCRIBED || this == UNSUBSCRIBED || this == NOT_FOUND

    companion object {
        fun fromValue(value: String?): MarketingConsentStatus =
            entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
