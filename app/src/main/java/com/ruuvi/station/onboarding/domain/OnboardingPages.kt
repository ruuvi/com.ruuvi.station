package com.ruuvi.station.onboarding.domain

enum class OnboardingPages (val gatewayRequired: Boolean = false) {
    MEASURE_YOUR_WORLD,
    READ_SENSORS_DATA,
    DASHBOARD,
    PERSONALISE,
    HISTORY,
    ALERTS,
    SHARING (true),
    WIDGETS (true),
    WEB (true),
    FINISH
}