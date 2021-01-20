package com.ruuvi.station.util

import android.content.Context
import com.ruuvi.station.R

object TimeUtils {
// Disabled for localization. Should be done with plurals later
//    <string name="scanner_notification_minute">minute</string>
//    <string name="scanner_notification_minutes">minutes</string>
//    <string name="scanner_notification_seconds">seconds</string>
//    <string name="scanner_notification_second">second</string>
//    fun convertSecondsToText(context: Context, scanInterval: Int): String {
//        var seconds = scanInterval
//        var stringSeconds = context.getString(R.string.scanner_notification_seconds)
//
//        if (seconds < 60) {
//            return "$seconds $stringSeconds"
//        } else {
//            val minutes = seconds / 60
//            seconds -= minutes * 60
//
//            val stringMinutes = if (minutes == 1) {
//                context.getString(R.string.scanner_notification_minute)
//            } else {
//                context.getString(R.string.scanner_notification_minutes)
//            }
//
//            stringSeconds = if (seconds == 1) {
//                context.getString(R.string.scanner_notification_second)
//            } else {
//                context.getString(R.string.scanner_notification_seconds)
//            }
//
//            return if (seconds > 0) {
//                "$minutes $stringMinutes $seconds $stringSeconds"
//            } else {
//                if (minutes > 1) "$minutes $stringMinutes" else stringMinutes
//            }
//        }
//    }

    fun convertSecondsToTextTemp(context: Context, scanInterval: Int): String {
        var seconds = scanInterval
        var stringSeconds = context.getString(R.string.sec)

        if (seconds < 60) {
            return "$seconds $stringSeconds"
        } else {
            val minutes = seconds / 60
            seconds -= minutes * 60

            val stringMinutes = context.getString(R.string.min)

            return if (seconds > 0) {
                "$minutes $stringMinutes $seconds $stringSeconds"
            } else {
                "$minutes $stringMinutes"
            }
        }
    }
}