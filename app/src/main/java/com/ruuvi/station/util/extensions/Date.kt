package com.ruuvi.station.util.extensions

import android.content.Context
import android.text.format.DateFormat
import com.ruuvi.station.R
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import kotlin.math.abs

const val hours24 = 24 * 60 * 60 * 1000L

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
    val localDate = LocalDateTime.ofInstant(toInstant(), TimeZone.getDefault().toZoneId()).with(
        LocalTime.MIN)
    return time == Date.from(localDate.atZone(ZoneId.systemDefault()).toInstant()).time
}

fun Date.localizedTime(context: Context): String {
    val timeFormat = DateFormat.getTimeFormat(context)
    return timeFormat.format(this)
}

fun Date.localizedDate(context: Context): String {
    val dateFormat = DateFormat.getDateFormat(context)
    return dateFormat.format(this)
}

fun Date.localizedDateTime(context: Context): String {
    val dateFormat = DateFormat.getDateFormat(context)
    val timeFormat = DateFormat.getTimeFormat(context)
    return "${dateFormat.format(this)} ${timeFormat.format(this)}"
}