package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.util.extensions.hours24
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.Date

class WidgetTimestampFormatterTest {
    private val context = mock<Context>()

    @Test
    fun `timestamp exactly 24 hours old uses localized time`() {
        val formatter = formatter(now = NOW)

        val result = formatter.format(NOW - hours24)

        assertEquals(FORMATTED_TIME, result)
    }

    @Test
    fun `timestamp more than 24 hours old uses localized date`() {
        val formatter = formatter(now = NOW)

        val result = formatter.format(NOW - hours24 - 1)

        assertEquals(FORMATTED_DATE, result)
    }

    @Test
    fun `timestamp more than 24 hours in future uses localized date`() {
        val formatter = formatter(now = NOW)

        val result = formatter.format(NOW + hours24 + 1)

        assertEquals(FORMATTED_DATE, result)
    }

    @Test
    fun `extreme timestamp distance is handled without overflowing`() {
        val formatter = formatter(now = Long.MAX_VALUE)

        val result = formatter.format(Long.MIN_VALUE)

        assertEquals(FORMATTED_DATE, result)
    }

    @Test
    fun `time formatter receives timestamp and provided context`() {
        val expectedTimestamp = NOW - 1
        var receivedTimestamp: Date? = null
        var receivedContext: Context? = null
        val formatter = LocalizedWidgetTimestampFormatter(
            context = context,
            currentTimeMillis = { NOW },
            dateFormatter = { _, _ -> FORMATTED_DATE },
            timeFormatter = { timestamp, formatterContext ->
                receivedTimestamp = timestamp
                receivedContext = formatterContext
                FORMATTED_TIME
            },
        )

        formatter.format(expectedTimestamp)

        assertEquals(Date(expectedTimestamp), receivedTimestamp)
        assertSame(context, receivedContext)
    }

    @Test
    fun `date formatter receives timestamp and provided context`() {
        val expectedTimestamp = NOW - hours24 - 1
        var receivedTimestamp: Date? = null
        var receivedContext: Context? = null
        val formatter = LocalizedWidgetTimestampFormatter(
            context = context,
            currentTimeMillis = { NOW },
            dateFormatter = { timestamp, formatterContext ->
                receivedTimestamp = timestamp
                receivedContext = formatterContext
                FORMATTED_DATE
            },
            timeFormatter = { _, _ -> FORMATTED_TIME },
        )

        formatter.format(expectedTimestamp)

        assertEquals(Date(expectedTimestamp), receivedTimestamp)
        assertSame(context, receivedContext)
    }

    private fun formatter(now: Long) = LocalizedWidgetTimestampFormatter(
        context = context,
        currentTimeMillis = { now },
        dateFormatter = { _, _ -> FORMATTED_DATE },
        timeFormatter = { _, _ -> FORMATTED_TIME },
    )

    companion object {
        private const val NOW = 10 * hours24
        private const val FORMATTED_TIME = "12:34"
        private const val FORMATTED_DATE = "2026-07-26"
    }
}
