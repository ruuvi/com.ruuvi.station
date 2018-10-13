package com.ruuvi.station.util

enum class BackgroundScanModes(val value: Int) {
    DISABLED(0), FOREGROUND(1), BACKGROUND(2);
    companion object {
        private val map = BackgroundScanModes.values().associateBy(BackgroundScanModes::value)
        fun fromInt(type: Int) = map[type]
    }
}