package com.ruuvi.station.app.locale

import android.content.Context
import android.os.Build

class LocaleInteractor(val context: Context) {
    fun getCurrentLocaleLanguage(): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales.get(0)
        } else {
            context.resources.configuration.locale
        }
        return locale.language
    }
}