package com.ruuvi.station.network.data.request

data class MarketingConsentRequest(
    val consent: Boolean,
    val silent: Boolean = true,
    val joiningSource: String = JOINING_SOURCE,
    val language: String
) {
    companion object {
        const val JOINING_SOURCE = "android"

        fun normalizeLanguage(language: String): String =
            language.trim().take(2).uppercase().takeIf { it.length == 2 } ?: "EN"
    }
}
