package com.ruuvi.station.util.extensions

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale

class DateTest {
    @Test
    fun `uses compatibility locale list when formatting English Finnish dates`() {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()
        val locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("FI")
            .build()
        val date = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 3, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        every { context.resources } returns resources
        every { resources.configuration } returns configuration
        mockkStatic(ConfigurationCompat::class)
        every { ConfigurationCompat.getLocales(configuration) } returns LocaleListCompat.create(locale)

        try {
            assertEquals("3.2.2024", date.localizedDate(context))
        } finally {
            unmockkStatic(ConfigurationCompat::class)
        }
    }

    @Test
    fun `falls back to the default locale when the compatibility locale list is empty`() {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()
        val locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("FI")
            .build()
        val date = Calendar.getInstance().apply {
            set(2024, Calendar.FEBRUARY, 3, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val previousDefaultLocale = Locale.getDefault()

        every { context.resources } returns resources
        every { resources.configuration } returns configuration
        mockkStatic(ConfigurationCompat::class)
        every { ConfigurationCompat.getLocales(configuration) } returns LocaleListCompat.getEmptyLocaleList()
        Locale.setDefault(locale)

        try {
            assertEquals("3.2.2024", date.localizedDate(context))
        } finally {
            Locale.setDefault(previousDefaultLocale)
            unmockkStatic(ConfigurationCompat::class)
        }
    }

    @Test
    fun `uses Finnish numeric date patterns for English Finland`() {
        val locale = Locale.Builder()
            .setLanguage("en")
            .setRegion("FI")
            .build()

        assertEquals("d.M.yyyy", localizedNumericDatePattern(locale, includeYear = true))
        assertEquals("d.M.", localizedNumericDatePattern(locale, includeYear = false))
    }

    @Test
    fun `keeps Android date patterns for other regions`() {
        assertNull(localizedNumericDatePattern(Locale.UK, includeYear = true))
    }

    @Test
    fun `keeps Android date patterns for Finnish Finland`() {
        assertNull(localizedNumericDatePattern(Locale.forLanguageTag("fi-FI"), includeYear = true))
    }
}
