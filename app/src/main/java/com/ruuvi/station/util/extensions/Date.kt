package com.ruuvi.station.util.extensions

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import com.ruuvi.station.R
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

const val hours24 = 24 * 60 * 60 * 1000L
private const val FINLAND_COUNTRY_CODE = "FI"
private val FINNISH_FINLAND_LOCALE = Locale("fi", FINLAND_COUNTRY_CODE)

fun Date.getEpochSecond(): Long {
    return this.time / 1000L
}

fun Date.diffGreaterThan(diff: Long): Boolean {
    return abs(Date().time - this.time) > diff
}

fun Date.describingTimeSince(context: Context): String {
    var output = ""
    val dateNow = Date()
    val diffInMS: Long = dateNow.time - this.time
    // show date if the tag has not been seen for 24h
    if (diffInMS > hours24) {
        output += this.localizedDateTime(context)
    } else {
        val seconds = (diffInMS / 1000).toInt() % 60
        val minutes = (diffInMS / (1000 * 60) % 60).toInt()
        val hours = (diffInMS / (1000 * 60 * 60) % 24).toInt()
        if (hours > 0) output += "$hours ${context.getString(R.string.h)} "
        if (minutes > 0) output += "$minutes ${context.getString(R.string.min)} "
        output += "$seconds ${context.getString(R.string.s)}"
        output = context.getString(R.string.time_since, output)
    }
    return output
}

fun Date.isStartOfTheDay(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return time == calendar.time.time
}

fun Date.localizedTime(context: Context): String {
    val timeFormat = DateFormat.getTimeFormat(context)
    return timeFormat.format(this)
}

fun Date.localizedDate(context: Context, includeYear: Boolean = true): String {
    val pattern = localizedNumericDatePattern(
        context.resources.configuration.locales[0],
        includeYear
    )
    if (pattern != null) {
        return SimpleDateFormat(pattern, FINNISH_FINLAND_LOCALE).format(this)
    }

    return if (includeYear) {
        DateFormat.getDateFormat(context).format(this)
    } else {
        DateUtils.formatDateTime(
            context,
            time,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
        )
    }
}

fun Date.localizedDateTime(context: Context): String {
    val dateFormat = DateFormat.getDateFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)
    return "${dateFormat.format(this)} ${timeFormat.format(this)}"
}

internal fun localizedNumericDatePattern(locale: Locale, includeYear: Boolean): String? {
    if (!locale.country.equals(FINLAND_COUNTRY_CODE, ignoreCase = true)) {
        return null
    }

    return if (includeYear) "d.M.yyyy" else "d.M."
}