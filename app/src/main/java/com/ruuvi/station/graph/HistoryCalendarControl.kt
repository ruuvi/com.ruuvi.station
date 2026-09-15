package com.ruuvi.station.graph

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.history.HistorySelection
import com.ruuvi.station.history.calendarDateUtc
import com.ruuvi.station.history.localMidnight
import com.ruuvi.station.network.domain.HistorySyncState
import java.text.DateFormat
import java.util.Date
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryCalendarControl(selection: HistorySelection, setDates: (Long, Long) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("historyCalendar")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, stringResource(R.string.history_select_dates),
                tint = RuuviStationTheme.colors.buttonText, modifier = Modifier.size(20.dp))
            Icon(Icons.Default.ArrowDropDown, null, tint = RuuviStationTheme.colors.accent,
                modifier = Modifier.size(18.dp))
        }
    }
    if (expanded) {
        val current = remember { System.currentTimeMillis() }
        val zone = remember { TimeZone.getDefault() }
        val minimumDate = calendarDateUtc(com.ruuvi.station.history.HistoryRange.retained(current).startMillis, zone)
        val maximumDate = calendarDateUtc(current, zone)
        val selected = selection.resolve(current, zone)
        val state = rememberDateRangePickerState(
            initialSelectedStartDateMillis = calendarDateUtc(selected.startMillis.coerceAtMost(current), zone),
            initialSelectedEndDateMillis = calendarDateUtc((selected.endExclusiveMillis - 1).coerceAtMost(current), zone),
            selectableDates = remember(minimumDate, maximumDate) {
                object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis in minimumDate..maximumDate
                }
            }
        )
        val colors = RuuviStationTheme.colors
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(
            primary = colors.accent, onPrimary = colors.buttonText,
            secondaryContainer = colors.accent.copy(alpha = 0.2f).compositeOver(colors.background),
            onSecondaryContainer = colors.primary,
            surface = colors.background, surfaceContainerHigh = colors.background,
            onSurface = colors.primary, onSurfaceVariant = colors.secondaryTextColor,
            outlineVariant = colors.divider
        )) {
            DatePickerDialog(
                onDismissRequest = { expanded = false },
                confirmButton = {
                    TextButton(
                        enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                        onClick = {
                            val start = state.selectedStartDateMillis
                            val end = state.selectedEndDateMillis
                            if (start != null && end != null) setDates(start, end)
                            expanded = false
                        }, modifier = Modifier.testTag("historyCalendarApply")
                    ) { Text(stringResource(R.string.history_apply)) }
                },
                dismissButton = {
                    TextButton(onClick = { expanded = false }) { Text(stringResource(android.R.string.cancel)) }
                }
            ) {
                DateRangePicker(
                    state = state,
                    title = { Text(stringResource(R.string.history_select_dates), modifier = Modifier.padding(16.dp)) },
                    headline = null,
                    showModeToggle = false,
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = (LocalConfiguration.current.screenHeightDp - 140).coerceAtLeast(180).dp)
                )
            }
        }
    }
}

@Composable
fun HistoryRangeCaption(selection: HistorySelection) {
    if (selection is HistorySelection.Custom) {
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val zone = TimeZone.getDefault()
        val start = dateFormat.format(Date(localMidnight(selection.startDateUtc, zone)))
        val end = dateFormat.format(Date(localMidnight(selection.endDateUtc, zone)))
        androidx.compose.material.Text(
            stringResource(R.string.history_selected_dates, start, end),
            color = RuuviStationTheme.colors.buttonText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).testTag("historyRangeCaption")
        )
    }
}

@Composable
fun CloudHistoryStatus(state: HistorySyncState, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().testTag("historyCloudLoading"))
            androidx.compose.material.Text(stringResource(R.string.history_cloud_loading), color = RuuviStationTheme.colors.buttonText)
        }
        if (state.failed) {
            androidx.compose.material.Text(stringResource(R.string.history_cloud_error), color = RuuviStationTheme.colors.buttonText)
            androidx.compose.material.TextButton(onClick = retry) {
                androidx.compose.material.Text(stringResource(R.string.try_again), color = RuuviStationTheme.colors.buttonText)
            }
        }
        if (state.restricted) {
            androidx.compose.material.Text(
                if (state.cloudHistoryDays == 0) stringResource(R.string.history_cloud_unavailable)
                else stringResource(R.string.history_cloud_limited, state.cloudHistoryDays ?: 0),
                color = RuuviStationTheme.colors.buttonText
            )
        }
    }
}
