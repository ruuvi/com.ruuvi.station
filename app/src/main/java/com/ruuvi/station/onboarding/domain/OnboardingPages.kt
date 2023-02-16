package com.ruuvi.station.onboarding.domain

enum class OnboardingPages (val gatewayRequired: Boolean = false) {
    MEASURE_YOUR_WORLD,
    DASHBOARD,
    PERSONALISE,
    HISTORY,
    ALERTS,
    SHARING (true),
    WIDGETS (true),
    WEB (true),
    FINISH
}