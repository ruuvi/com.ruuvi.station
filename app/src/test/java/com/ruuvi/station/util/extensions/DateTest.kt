package com.ruuvi.station.util.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class DateTest {
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
}
