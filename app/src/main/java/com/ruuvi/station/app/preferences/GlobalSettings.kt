package com.ruuvi.station.app.preferences

object GlobalSettings {
    // Rolling 365-day years, matching iOS. All retention and export paths share this limit.
    const val historyLengthDays: Int = 3 * 365
    const val historyLengthHours: Int = historyLengthDays * 24
    const val historyLengthMillis: Long = historyLengthHours * 60L * 60 * 1000
}
