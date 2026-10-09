package com.ruuvi.station.vico

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.ruuvi.station.util.extensions.isStartOfTheDay
import com.ruuvi.station.util.extensions.localizedDate
import java.text.DateFormat
import java.util.Date

@Composable
fun rememberDateFormatter(): CartesianValueFormatter {
    val context = LocalContext.current
    return remember {
        CartesianValueFormatter { measureContext, value, verticalAxisPosition ->
            val date = Date(value.toLong())
            if (date.isStartOfTheDay()) {
                date.localizedDate(context, includeYear = false)
            } else {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(date).replace(" ", "")
            }
        }
    }
}