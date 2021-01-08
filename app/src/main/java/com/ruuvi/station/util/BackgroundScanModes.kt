package com.ruuvi.station.util

import com.ruuvi.station.R

enum class BackgroundScanModes(val value: Int) {

    DISABLED(0) {
        override val label: Int
            get() = R.string.settings_background_disabled
        override val description: Int
            get() = R.string.settings_background_scan_details
    },
    BACKGROUND(1) {
        override val label: Int
            get() = R.string.settings_background_continuous
        override val description: Int
            get() = R.string.settings_background_scan_details_continuous
    };

    abstract val label: Int
    abstract val description: Int

    companion object {
        private val map = values().associateBy(BackgroundScanModes::value)
        fun fromInt(type: Int) = map[type]
    }
}