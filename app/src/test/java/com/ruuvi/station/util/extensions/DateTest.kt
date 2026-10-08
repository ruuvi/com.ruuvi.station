package com.ruuvi.station.util.extensions

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DateTest {
    @Test
    fun `returns epoch seconds and identifies the start of the day`() {
        val midnight = dateAt(hour = 0)

        assertEquals(midnight.time / 1000, midnight.getEpochSecond())
        assertTrue(midnight.isStartOfTheDay())
        assertFalse(dateAt(hour = 12).isStartOfTheDay())
    }

    @Test
    fun `compares dates against a duration`() {
        val date = Date(System.currentTimeMillis() - 10_000)

        assertTrue(date.diffGreaterThan(1_000))
        assertFalse(date.diffGreaterThan(60_000))
    }

    @Test
    fun `describes recent timestamps using elapsed time`() {
        val context = mockk<Context>(relaxed = true)
        val date = Date(System.currentTimeMillis() - (3_723 * 1_000))

        date.describingTimeSince(context)
    }

    @Test
    fun `describes old timestamps using localized date and time`() {
        val context = contextWithLocale(englishFinnishLocale())
        val date = Date(System.currentTimeMillis() - hours24 - 1_000)

        mockkStatic(DateFormat::class)
        every { DateFormat.getTimeFormat(context) } returns SimpleDateFormat("HH:mm")

        try {
            date.describingTimeSince(context)
        } finally {
            unmockkStatic(DateFormat::class)
            unmockkStatic(ConfigurationCompat::class)
        }
    }

    @Test
    fun `uses Android date format fallbacks for unsupported locales`() {
        val context = contextWithLocale(Locale.UK)
        val date = dateAt(hour = 12)
        val noYearFlags = DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_NO_YEAR or
                DateUtils.FORMAT_NUMERIC_DATE

        mockkStatic(DateFormat::class)
        mockkStatic(DateUtils::class)
        every { DateFormat.getDateFormat(context) } returns SimpleDateFormat("'fallback-date'")
        every { DateUtils.formatDateTime(context, date.time, noYearFlags) } returns "fallback-date"

        try {
            assertEquals("fallback-date", date.localizedDate(context))
            assertEquals("fallback-date", date.localizedDate(context, includeYear = false))
        } finally {
            unmockkStatic(DateUtils::class)
            unmockkStatic(DateFormat::class)
            unmockkStatic(ConfigurationCompat::class)
        }
    }

    @Test
    fun `formats localized time and date time`() {
        val context = contextWithLocale(englishFinnishLocale())
        val date = dateAt(hour = 12)

        mockkStatic(DateFormat::class)
        every { DateFormat.getTimeFormat(context) } returns SimpleDateFormat("'12:00'")

        try {
            assertEquals("12:00", date.localizedTime(context))
            assertEquals("3.2.2024 12:00", date.localizedDateTime(context))
        } finally {
            unmockkStatic(DateFormat::class)
            unmockkStatic(ConfigurationCompat::class)
        }
    }

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
        val locale = englishFinnishLocale()

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

    private fun contextWithLocale(locale: Locale): Context {
        val context = mockk<Context>()
        val resources = mockk<Resources>()
        val configuration = mockk<Configuration>()

        every { context.resources } returns resources
        every { resources.configuration } returns configuration
        mockkStatic(ConfigurationCompat::class)
        every { ConfigurationCompat.getLocales(configuration) } returns LocaleListCompat.create(locale)

        return context
    }

    private fun englishFinnishLocale(): Locale = Locale.Builder()
        .setLanguage("en")
        .setRegion("FI")
        .build()

    private fun dateAt(hour: Int): Date = Calendar.getInstance().apply {
        set(2024, Calendar.FEBRUARY, 3, hour, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
}
