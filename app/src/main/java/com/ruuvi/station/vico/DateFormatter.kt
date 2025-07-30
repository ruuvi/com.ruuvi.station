package com.ruuvi.station.vico

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.ruuvi.station.util.extensions.isStartOfTheDay
import java.text.DateFormat
import java.util.Date

@Composable
fun rememberDateFormatter(): CartesianValueFormatter {
    val context = LocalContext.current
    return remember {
        CartesianValueFormatter { measureContext, value, verticalAxisPosition ->
            val date = Date(value.toLong())
            if (date.isStartOfTheDay()) {
                val flags: Int =
                    DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_NO_YEAR or DateUtils.FORMAT_NUMERIC_DATE
                DateUtils.formatDateTime(context, date.time, flags)
            } else {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ", "")
            }
        }
    }
}