package com.ruuvi.station.app.preferences

object GlobalSettings {
    const val historyLengthDays: Int = 100
    const val historyLengthHours: Int = historyLengthDays * 24
    const val historyLengthMillis: Long = historyLengthHours * 60L * 60 * 1000
}
