package com.ruuvi.station.graph

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build.VERSION
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.components.dialog.RuuviConfirmDialog
import com.ruuvi.station.app.ui.components.dialog.MessageDialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.tagdetails.ui.SyncStatus
import com.ruuvi.station.util.Period
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.ruuvi.station.history.HistoryWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

private const val CHART_SIZE_LEVEL_NORMAL = 1
private const val CHART_SIZE_LEVEL_MAX = 3
private val ChartToolbarHeight = 36.dp

@Composable
fun ChartControlElement2(
    sensorId: String,
    viewPeriod: Period,
    showChartStats: Boolean,
    syncStatus: Flow<SyncStatus>,
    chartSizeLevel: Int,
    hideIncreaseChartSize: Boolean,
    increaseChartSize: () -> Unit,
    decreaseChartSize: () -> Unit,
    disconnectGattAction: (String) -> Unit,
    shouldSkipGattSyncDialog: () -> Boolean,
    syncGatt: (String) -> Unit,
    setViewPeriod: (Int) -> Unit,
    exportToCsv: suspend (String) -> Uri?,
    exportToXlsx: suspend (String) -> Uri?,
    removeTagData: (String) -> Unit,
    refreshStatus: () -> Unit,
    dontShowGattSyncDescription: () -> Unit,
    changeShowStats: () -> Unit,
    historyEnabled: Boolean = false,
    historyDays: Int = 0,
    historyPresets: Boolean = true,
    historyCustom: HistoryWindow? = null,
    historyTimeline: HistoryWindow? = null,
    historySelect: (HistoryWindow) -> Unit = {},
    exporting: Boolean = false,
    historyProgress: Float? = null
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val context = LocalContext.current

    var gattSyncDialogOpened by remember {
        mutableStateOf(false)
    }

    var disconnectDialogOpened by remember {
        mutableStateOf(false)
    }

    var syncInProgress by remember {
        mutableStateOf(false)
    }

    var syncMessage by remember {
        mutableStateOf<UiText>(UiText.EmptyString)
    }

    var uiEvent by remember {
        mutableStateOf<SyncProgress?>(null)
    }

    LaunchedEffect(key1 = true) {
        syncStatus.collectLatest { event ->
            Timber.d("SyncEvent collected $event")

            if (event.manualSync && listOf(
                    SyncProgress.ERROR,
                    SyncProgress.NOT_FOUND,
                    SyncProgress.NOT_SUPPORTED,
                    SyncProgress.DISCONNECTED
                ).contains(event.syncProgress)
            ) {
                uiEvent = event.syncProgress
            }
            syncInProgress = event.syncInProgress
            syncMessage = event.statusMessage
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().height(ChartToolbarHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HistorySyncControl(
            modifier = Modifier.weight(1f),
            bluetoothSyncing = syncInProgress,
            bluetoothMessage = syncMessage.asString(context),
            cloudProgress = historyProgress,
            cancelBluetooth = { disconnectDialogOpened = true },
            sync = {
                if (!shouldSkipGattSyncDialog()) gattSyncDialogOpened = true
                syncGatt(sensorId)
            }
        )

        Row(
            modifier = Modifier.wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            if (historyEnabled) {
                HistoryCalendar(historyDays, historyCustom ?: historyTimeline, historySelect)
                HistoryDurationMenu(viewPeriod, historyDays, historyPresets, historyCustom, setViewPeriod)
            } else ViewPeriodMenu(viewPeriod, setViewPeriod)

            ThreeDotsMenu(
                sensorId = sensorId,
                showChartStats = showChartStats,
                chartSizeLevel = chartSizeLevel,
                hideIncreaseChartSize = hideIncreaseChartSize,
                increaseChartSize = increaseChartSize,
                decreaseChartSize = decreaseChartSize,
                exportToCsv = { exportToCsv(sensorId) },
                exportToXlsx = { exportToXlsx(sensorId) },
                clearHistory = { removeTagData(sensorId) },
                changeShowStats = changeShowStats,
                exportEnabled = !exporting && (!historyEnabled || historyTimeline != null)
            )
        }
    }

    DisposableEffect( key1 = lifecycleOwner ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    if (uiEvent != null) {
        when (uiEvent) {
            SyncProgress.DISCONNECTED -> {
                uiEvent = null
            }
            SyncProgress.NOT_SUPPORTED -> {
                MessageDialog(message = stringResource(id = R.string.reading_history_not_supported)) {
                    uiEvent = null
                }
            }
            SyncProgress.NOT_FOUND, SyncProgress.ERROR -> {
                if (!gattSyncDialogOpened) {
                    RuuviConfirmDialog(
                        title = stringResource(id = R.string.gatt_download_failed),
                        message = stringResource(id = R.string.gatt_not_in_range_description),
                        noButtonCaption = stringResource(id = R.string.close),
                        yesButtonCaption = stringResource(id = R.string.try_again),
                        onDismissRequest = {
                            uiEvent = null
                        }
                    ) {
                        uiEvent = null
                        syncGatt(sensorId)
                    }
                }
            }
            else -> {
                uiEvent = null
            }
        }
    }

    if (gattSyncDialogOpened) {
        GattSyncDescriptionDialog(
            doNotShowAgain = {
                gattSyncDialogOpened = false
                dontShowGattSyncDescription()
            },
            onDismissRequest = {
                gattSyncDialogOpened = false
            }
        )
    }

    if (disconnectDialogOpened) {
        DisconnectConfirmDialog(
            onDisconnesct = {
                disconnectGattAction(sensorId)
            },
            onDismissRequest = {
                disconnectDialogOpened = false
            }
        )
    }
}

@Composable
fun ViewPeriodMenu(
    viewPeriod: Period,
    setViewPeriod: (Int) -> Unit
) {
    var daysMenuExpanded by remember {
        mutableStateOf(false)
    }

    var showMoreDialog by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.clickable { daysMenuExpanded = true }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val daysText = if (viewPeriod.shouldPassValue) {
                stringResource(id = viewPeriod.stringResourceId, viewPeriod.value)
            } else {
                stringResource(id = viewPeriod.stringResourceId)
            }
            Text(
                style = RuuviStationTheme.typography.subtitle,
                text = daysText,
                color = RuuviStationTheme.colors.buttonText
            )

            Icon(
                painter = painterResource(id = R.drawable.drop_down_24),
                contentDescription = "",
                tint = RuuviStationTheme.colors.accent
            )
        }
        DarkChartDropdownMenu(
            expanded = daysMenuExpanded,
            onDismissRequest = { daysMenuExpanded = false },
        ) {
            val periodOptions = listOf(
                Period.All,
                Period.Hour1,
                Period.Hour2,
                Period.Hour3,
                Period.Hour6,
                Period.Hour12,
                Period.Day1,
                Period.Day2,
                Period.Day3,
                Period.Day4,
                Period.Day5,
                Period.Day6,
                Period.Day7,
                Period.Day8,
                Period.Day9,
                Period.Day10,
            )
            for (day in periodOptions) {
                DropdownMenuItem(onClick = {
                    setViewPeriod.invoke(day.value)
                    daysMenuExpanded = false
                }) {
                    Paragraph(text = stringResource(id = day.stringResourceId))
                }
            }

            DropdownMenuItem(onClick = {
                showMoreDialog = true
                daysMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.more))
            }
        }
    }

    if (showMoreDialog) {
        MessageDialog(
            title = stringResource(id = R.string.longer_history_title),
            message = stringResource(id = R.string.longer_history_message)
        ) {
            showMoreDialog = false
        }
    }
}

@Composable
fun ThreeDotsMenu(
    sensorId: String,
    showChartStats: Boolean,
    chartSizeLevel: Int,
    hideIncreaseChartSize: Boolean,
    increaseChartSize: () -> Unit,
    decreaseChartSize: () -> Unit,
    exportToCsv: suspend () -> Uri?,
    exportToXlsx: suspend () -> Uri?,
    clearHistory: () -> Unit,
    changeShowStats: () -> Unit,
    exportEnabled: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var exportFailed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var clearConfirmOpened by remember {
        mutableStateOf(false)
    }
    var android26RequiredDialog by remember {
        mutableStateOf(false)
    }
    var threeDotsMenuExpanded by remember {
        mutableStateOf(false)
    }

    Box() {
        IconButton(onClick = { threeDotsMenuExpanded = !threeDotsMenuExpanded }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_3dots),
                contentDescription = null,
                tint = RuuviStationTheme.colors.buttonText
            )
        }

        DarkChartDropdownMenu(
            expanded = threeDotsMenuExpanded,
            onDismissRequest = { threeDotsMenuExpanded = false },
        ) {
            DropdownMenuItem(enabled = exportEnabled, onClick = {
                threeDotsMenuExpanded = false
                scope.launch {
                    try { exportToCsv()?.let { sendCsv(sensorId, it, context) } }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { exportFailed = true }
                }
            }) {
                Paragraph(text = stringResource(id = R.string.export_history))
            }
            DropdownMenuItem(enabled = exportEnabled, onClick = {
                if (VERSION.SDK_INT < 26) {
                    android26RequiredDialog = true
                } else {
                    threeDotsMenuExpanded = false
                    scope.launch {
                        try { exportToXlsx()?.let { sendXlsx(sensorId, it, context) } }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { exportFailed = true }
                    }
                }
            }) {
                Paragraph(text = stringResource(id = R.string.export_history_xlsx))
            }
            DropdownMenuItem(onClick = {
                clearConfirmOpened = true
                threeDotsMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.clear_view))
            }
            DropdownMenuItem(onClick = {
                threeDotsMenuExpanded = false
                changeShowStats()
            }) {
                val caption = if (showChartStats) {
                    stringResource(id = R.string.chart_stat_hide)
                } else {
                    stringResource(id = R.string.chart_stat_show)
                }
                Paragraph(text = caption)
            }
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT &&
                !hideIncreaseChartSize && chartSizeLevel < CHART_SIZE_LEVEL_MAX) {
                DropdownMenuItem(onClick = {
                    threeDotsMenuExpanded = false
                    increaseChartSize()
                }) {
                    Paragraph(text = stringResource(id = R.string.increase_graph_size))
                }
            }
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT &&
                !hideIncreaseChartSize && chartSizeLevel > CHART_SIZE_LEVEL_NORMAL) {
                DropdownMenuItem(onClick = {
                    threeDotsMenuExpanded = false
                    decreaseChartSize()
                }) {
                    Paragraph(text = stringResource(id = R.string.decrease_graph_size))
                }
            }
        }
    }

    if (exportFailed) MessageDialog(message = stringResource(R.string.history_export_failed)) { exportFailed = false }

    if (clearConfirmOpened) {
        ClearHistoryDialog(
            clearHistory = clearHistory,
            dismissAction = { clearConfirmOpened = false }
        )
    }

    if (android26RequiredDialog) {
        MessageDialog(
            message = stringResource(id = R.string.android_8_required),
            onDismissRequest = { android26RequiredDialog = false }
        )
    }
}

@Composable
internal fun DarkChartDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    RuuviTheme(darkTheme = true) {
        CompositionLocalProvider(LocalElevationOverlay provides null) {
            MaterialTheme(
                colors = MaterialTheme.colors.copy(
                    surface = RuuviStationTheme.colors.background,
                ),
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = onDismissRequest,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun GattSyncDescriptionDialog(
    onDismissRequest : () -> Unit,
    doNotShowAgain: () -> Unit,
) {

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.extended)
                .fillMaxWidth(),
            shape = RoundedCornerShape(RuuviStationTheme.dimensions.medium),
            backgroundColor = RuuviStationTheme.colors.background
        )
        {
            Column(
                modifier = Modifier
                    .padding(all = RuuviStationTheme.dimensions.extended)
            ) {
                SubtitleWithPadding(text = stringResource(id = R.string.synchronisation))

                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
                
                ParagraphWithPadding(text = stringResource(id = R.string.gatt_sync_description))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    RuuviTextButton(
                        text = stringResource(id = R.string.do_not_show_again),
                        onClick = {
                            doNotShowAgain.invoke()
                        }
                    )

                    RuuviTextButton(
                        text = stringResource(id = R.string.close),
                        onClick = {
                            onDismissRequest.invoke()
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
            }
        }
    }
}

@Composable
fun DisconnectConfirmDialog(
    onDisconnesct: () -> Unit,
    onDismissRequest : () -> Unit,
) {
    RuuviConfirmDialog(
        title = stringResource(id = R.string.dialog_are_you_sure),
        message = stringResource(id = R.string.gatt_please_wait),
        noButtonCaption = stringResource(id = R.string.ok),
        yesButtonCaption = stringResource(id = R.string.gatt_abort_download),
        onDismissRequest = onDismissRequest
    ) {
        onDismissRequest.invoke()
        onDisconnesct.invoke()
    }
}

@Composable
fun ClearHistoryDialog(
    clearHistory: () -> Unit,
    dismissAction: () -> Unit
) {
    RuuviConfirmDialog(
        title = stringResource(id = R.string.clear_local_history),
        message = stringResource(id = R.string.clear_local_history_description),
        yesButtonCaption = stringResource(id = R.string.clear),
        noButtonCaption = stringResource(id = R.string.cancel),
        onDismissRequest = { dismissAction.invoke() }
    ) {
        dismissAction.invoke()
        clearHistory.invoke()
    }
}

fun sendXlsx(sensorId: String, uri: Uri, context: Context) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sendIntent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_csv_chooser_title, sensorId)))
}

fun sendCsv(sensorId: String, uri: Uri, context: Context) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sendIntent.type = "text/csv"
    sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_csv_chooser_title, sensorId)))
}
