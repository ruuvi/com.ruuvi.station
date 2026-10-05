package com.ruuvi.station.settings.ui

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsListTest {
    private val supportedLocales = listOf(
        Locale.ENGLISH,
        Locale.GERMAN,
        Locale("fi"),
        Locale.FRENCH,
        Locale("sv"),
        Locale("pl")
    )

    @Test
    fun `unsupported system language resolves to English`() {
        assertEquals(
            Locale.ENGLISH,
            resolveAppLanguageLocale(listOf(Locale("bs")), supportedLocales)
        )
    }

    @Test
    fun `supported system language resolves to supported locale`() {
        assertEquals(
            Locale("fi"),
            resolveAppLanguageLocale(listOf(Locale("fi")), supportedLocales)
        )
    }
}
