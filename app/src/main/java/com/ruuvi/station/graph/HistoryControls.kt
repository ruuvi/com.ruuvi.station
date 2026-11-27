package com.ruuvi.station.graph

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.history.HistoryWindow
import com.ruuvi.station.util.Period
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal fun historyDurationHours(days: Int, presets: Boolean): List<Int> {
    val bound = days.coerceIn(0, HistoryWindow.MAX_DAYS) * 24
    val values = if (presets) listOf(1, 2, 3, 6, 12, 24, 48, 72, 168, 240, 336, 720, 2160, 4320, 8760, 17520, 26304)
        else listOf(1, 2, 3, 6, 12) + (1..days.coerceIn(0, HistoryWindow.MAX_DAYS)).map { it * 24 }
    return listOf(0) + values.filter { it <= bound }
}

@Composable
private fun durationLabel(hours: Int, presets: Boolean): String {
    val countAndUnit = if (presets) when (hours) {
        168 -> 1 to R.string.week; 336 -> 2 to R.string.weeks
        720 -> 1 to R.string.month; 2160 -> 3 to R.string.months; 4320 -> 6 to R.string.months
        8760 -> 1 to R.string.year; 17520 -> 2 to R.string.years; 26304 -> 3 to R.string.years
        else -> null
    } else null
    if (countAndUnit != null) return "${countAndUnit.first} ${stringResource(countAndUnit.second).lowercase(LocalConfiguration.current.locales[0])}"
    return when {
        hours == 0 -> stringResource(R.string.all)
        hours == 24 -> stringResource(R.string.day_1)
        hours % 24 == 0 -> stringResource(R.string.day_x, hours / 24)
        hours == 1 -> stringResource(R.string.hour_1)
        else -> stringResource(R.string.hour_x, hours)
    }
}

@Composable
fun HistoryDurationMenu(period: Period, days: Int, presets: Boolean, custom: HistoryWindow?, select: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.clickable { expanded = true }.testTag("historyDuration"), verticalAlignment = Alignment.CenterVertically) {
            Text(if (custom == null) durationLabel(period.value, presets) else stringResource(R.string.history_custom_dates),
                style = RuuviStationTheme.typography.subtitle, color = RuuviStationTheme.colors.buttonText)
            Icon(painterResource(R.drawable.drop_down_24), null, tint = RuuviStationTheme.colors.accent)
        }
        if (presets) DarkChartDropdownMenu(expanded, { expanded = false }) {
            historyDurationHours(days, true).forEach { hours ->
                DropdownMenuItem(onClick = { select(hours); expanded = false }) {
                    Text(durationLabel(hours, true), color = if (custom == null && period.value == hours)
                        RuuviStationTheme.colors.accent else RuuviStationTheme.colors.buttonText)
                }
            }
        }
    }
    if (expanded && !presets) Dialog(onDismissRequest = { expanded = false }) {
        Surface(color = RuuviStationTheme.colors.background, shape = MaterialTheme.shapes.medium) {
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * .65f).dp)) {
                items(historyDurationHours(days, presets)) { hours ->
                    DropdownMenuItem(onClick = { select(hours); expanded = false }) {
                        Text(durationLabel(hours, presets), color = if (custom == null && period.value == hours)
                            RuuviStationTheme.colors.accent else RuuviStationTheme.colors.buttonText)
                    }
                }
            }
        }
    }
}

/** Material's date picker uses UTC day identifiers, not instants in the user's timezone. */
internal fun calendarUtcDay(millis: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val local = Calendar.getInstance(zone).apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
internal fun calendarLocalDay(utcDay: Long, next: Boolean = false, zone: TimeZone = TimeZone.getDefault()): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcDay }
    return Calendar.getInstance(zone).apply {
        clear(); set(utc.get(Calendar.YEAR), utc.get(Calendar.MONTH), utc.get(Calendar.DAY_OF_MONTH))
        if (next) add(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCalendar(days: Int, window: HistoryWindow?, select: (HistoryWindow) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = days > 0, modifier = Modifier.testTag("historyCalendar")) {
        Icon(Icons.Default.CalendarToday, stringResource(R.string.history_custom_dates), tint = RuuviStationTheme.colors.buttonText,
            modifier = Modifier.size(20.dp))
    }
    if (expanded) {
        val now = remember { System.currentTimeMillis() }
        val earliest = now / 1000 - days.coerceIn(0, HistoryWindow.MAX_DAYS) * 86400L
        val first = calendarUtcDay(earliest * 1000); val last = calendarUtcDay(now)
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = calendarUtcDay(window?.since?.times(1000) ?: now).coerceIn(first, last),
            initialSelectedEndDateMillis = calendarUtcDay(window?.until?.times(1000) ?: now).coerceIn(first, last),
            selectableDates = remember(first, last) { object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in first..last
            } }
        )
        val colors = RuuviStationTheme.colors
        androidx.compose.material3.MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(
            primary = colors.accent, surface = colors.background, surfaceContainerHigh = colors.background,
            onSurface = colors.buttonText, onSurfaceVariant = colors.buttonText
        )) {
            DatePickerDialog(onDismissRequest = { expanded = false }, confirmButton = {
                TextButton(enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null, onClick = {
                    val start = state.selectedStartDateMillis; val end = state.selectedEndDateMillis
                    if (start != null && end != null) select(HistoryWindow(
                        maxOf(earliest, calendarLocalDay(start) / 1000),
                        minOf(now / 1000, calendarLocalDay(end, next = true) / 1000 - 1)))
                    expanded = false
                }) { Text(stringResource(android.R.string.ok), color = colors.accent) }
            }, dismissButton = {
                TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.cancel), color = colors.accent) }
            }) {
                DateRangePicker(state, title = { Text(stringResource(R.string.history_custom_dates), modifier = Modifier.padding(16.dp), color = colors.buttonText) },
                    headline = null, showModeToggle = false, modifier = Modifier.fillMaxWidth()
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp - 140).coerceAtLeast(180).dp))
            }
        }
    }
}

/** Persistent selection details; transient work/errors use dialogs so the plot never shifts. */
@Composable
fun HistoryStatus(failed: Boolean, exporting: Boolean, custom: HistoryWindow?, retry: () -> Unit, cancel: () -> Unit) {
    val view = LocalView.current
    DisposableEffect(view, exporting) {
        val previous = view.keepScreenOn
        if (exporting) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    if (custom != null) {
        val format = DateFormat.getDateInstance(DateFormat.MEDIUM)
        Text("${format.format(Date(custom.since * 1000))} – ${format.format(Date(custom.until * 1000))}",
            modifier = Modifier.fillMaxWidth().padding(horizontal = RuuviStationTheme.dimensions.screenPadding),
            color = RuuviStationTheme.colors.buttonText, style = RuuviStationTheme.typography.syncStatusText)
    }
    var dismissed by remember(failed) { mutableStateOf(false) }
    if (exporting) {
        AlertDialog(onDismissRequest = cancel,
            backgroundColor = RuuviStationTheme.colors.background,
            contentColor = RuuviStationTheme.colors.buttonText,
            text = { Text(stringResource(R.string.history_preparing_export)) },
            confirmButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.cancel), color = RuuviStationTheme.colors.accent) } })
    } else if (failed && !dismissed) {
        AlertDialog(onDismissRequest = { dismissed = true },
            backgroundColor = RuuviStationTheme.colors.background,
            contentColor = RuuviStationTheme.colors.buttonText,
            text = { Text(stringResource(R.string.history_load_failed)) },
            confirmButton = { TextButton(onClick = { dismissed = true; retry() }) { Text(stringResource(R.string.try_again), color = RuuviStationTheme.colors.accent) } },
            dismissButton = { TextButton(onClick = { dismissed = true }) { Text(stringResource(R.string.close), color = RuuviStationTheme.colors.accent) } })
    }
}
