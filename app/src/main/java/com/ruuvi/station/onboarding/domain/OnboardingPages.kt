package com.ruuvi.station.onboarding.domain

import com.ruuvi.station.R

enum class OnboardingPages (val backgroundImageRes: Int) {
    MEASURE_YOUR_WORLD (R.drawable.onboarding_bg_light),
    READ_SENSORS_DATA (R.drawable.onboarding_bg_dark),
    DASHBOARD (R.drawable.onboarding_bg_dark),
    PERSONALISE (R.drawable.onboarding_bg_light),
    HISTORY (R.drawable.onboarding_bg_light),
    ALERTS (R.drawable.onboarding_bg_dark),
    MESSAGES (R.drawable.onboarding_bg_dark),
    SHARING (R.drawable.onboarding_bg_light),
}