package com.ruuvi.station.util.extensions

import android.content.Context
import android.text.format.DateFormat
import com.ruuvi.station.R
import java.util.*
import kotlin.math.abs

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
    if (diffInMS > 24 * 60 * 60 * 1000) {
        val dateFormat = DateFormat.getDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        output += "${dateFormat.format(this)} ${timeFormat.format(this)}"
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