package com.ruuvi.station.app.ui

import androidx.appcompat.app.AppCompatDelegate
import com.ruuvi.station.R

enum class DarkModeState(val code: Int, val title: Int) {
    SYSTEM_THEME(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, R.string.follow_system_theme),
    DARK_THEME(AppCompatDelegate.MODE_NIGHT_YES, R.string.dark_theme),
    LIGHT_THEME(AppCompatDelegate.MODE_NIGHT_NO, R.string.light_theme)
}