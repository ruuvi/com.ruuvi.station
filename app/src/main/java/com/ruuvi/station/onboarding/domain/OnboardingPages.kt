package com.ruuvi.station.onboarding.domain

import com.ruuvi.station.R

enum class OnboardingPages (val backgroundImageRes: Int) {
    MEASURE_YOUR_WORLD (R.drawable.onboarding_bg_light),
    READ_SENSORS_DATA (R.drawable.onboarding_bg_dark),
    PAGE1 (R.drawable.onboarding_bg_dark),
    PAGE2 (R.drawable.onboarding_bg_dark),
    PAGE3 (R.drawable.onboarding_bg_dark),
}