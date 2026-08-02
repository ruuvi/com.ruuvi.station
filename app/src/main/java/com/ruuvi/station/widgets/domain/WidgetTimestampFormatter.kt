package com.ruuvi.station.widgets.domain

import android.content.Context
import com.ruuvi.station.util.extensions.hours24
import com.ruuvi.station.util.extensions.localizedDate
import com.ruuvi.station.util.extensions.localizedTime
import java.util.Date

internal fun interface WidgetTimestampFormatter {
    fun format(timestampEpochMillis: Long): String
}

internal class LocalizedWidgetTimestampFormatter(
    private val context: Context,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val dateFormatter: (Date, Context) -> String = { date, context ->
        date.localizedDate(context)
    },
    private val timeFormatter: (Date, Context) -> String = { date, context ->
        date.localizedTime(context)
    },
) : WidgetTimestampFormatter {
    override fun format(timestampEpochMillis: Long): String {
        val timestamp = Date(timestampEpochMillis)
        return if (isMoreThanOneDayFromNow(timestampEpochMillis)) {
            dateFormatter(timestamp, context)
        } else {
            timeFormatter(timestamp, context)
        }
    }

    private fun isMoreThanOneDayFromNow(timestampEpochMillis: Long): Boolean {
        val now = currentTimeMillis()
        return if (timestampEpochMillis >= now) {
            now <= Long.MAX_VALUE - hours24 && timestampEpochMillis > now + hours24
        } else {
            now >= Long.MIN_VALUE + hours24 && timestampEpochMillis < now - hours24
        }
    }
}
