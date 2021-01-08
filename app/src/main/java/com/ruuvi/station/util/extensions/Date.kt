package com.ruuvi.station.util.extensions

import android.content.Context
import com.ruuvi.station.R
import java.util.*

fun Date.describingTimeSince(context: Context): String {
    var output = ""
    val dateNow = Date()
    val diffInMS: Long = dateNow.time - this.time
    // show date if the tag has not been seen for 24h
    if (diffInMS > 24 * 60 * 60 * 1000) {
        output += this.toString()
    } else {
        val seconds = (diffInMS / 1000).toInt() % 60
        val minutes = (diffInMS / (1000 * 60) % 60).toInt()
        val hours = (diffInMS / (1000 * 60 * 60) % 24).toInt()
        if (hours > 0) output += "$hours ${context.getString(R.string.h)} "
        if (minutes > 0) output += "$minutes ${context.getString(R.string.min)} "
        output += "$seconds ${context.getString(R.string.s)} ${context.getString(R.string.ago)}"
    }
    return output
}