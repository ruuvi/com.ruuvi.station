package com.ruuvi.station.graph

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.bluetooth.model.SyncProgress
import com.ruuvi.station.tagdetails.ui.TagViewModel
import com.ruuvi.station.util.Days

@Composable
fun ChartControlElement(
    viewModel: TagViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val context = LocalContext.current

    val viewPeriod by viewModel.chartViewPeriod.collectAsState(Days.Day10())

    val canUseGatt by viewModel.canUseGattSync.observeAsState(false)

    val syncInProgress by viewModel.gattSyncInProgress.collectAsState(false)

    val syncMessage by viewModel.gattSyncStatus.collectAsState(UiText.EmptyString)

    var gattSyncDialogOpened by remember {
        mutableStateOf(false)
    }

    val dontShowGattSync by viewModel.dontShowGattSync.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (syncInProgress) {
            IconButton(onClick = {
                viewModel.disconnectGatt()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_clear_24),
                    contentDescription = null,
                    tint = RuuviStationTheme.colors.buttonText
                )
            }
            Text(
                style = RuuviStationTheme.typography.syncStatusText,
                text = syncMessage.asString(context)
            )
        } else {
            IconButton(onClick = {
                gattSyncDialogOpened = true
                viewModel.syncGatt()
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = RuuviStationTheme.dimensions.medium)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.gatt_sync),
                        contentDescription = null,
                        tint = RuuviStationTheme.colors.buttonText
                    )
                    Spacer(modifier = Modifier.width(RuuviStationTheme.dimensions.medium))
                    Text(
                        style = RuuviStationTheme.typography.subtitle,
                        text = stringResource(id = R.string.sync),
                        color = RuuviStationTheme.colors.buttonText
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            ViewPeriodMenu(
                viewPeriod = viewPeriod,
                setViewPeriod = viewModel::setViewPeriod
            )

            ThreeDotsMenu(
                sensorId = viewModel.sensorId,
                exportToCsv = viewModel::exportToCsv,
                clearHistory = viewModel::removeTagData
            )
        }
    }

    DisposableEffect( key1 = lifecycleOwner ) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshStatus()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var uiEvent by remember {
        mutableStateOf<SyncProgress?>(null)
    }

    LaunchedEffect(key1 = true) {
        viewModel.event.collect() { event ->
            uiEvent = event
        }
    }
     if (uiEvent != null) {
         when (uiEvent) {
             SyncProgress.DISCONNECTED -> {
                 RuuviMessageDialog(message = stringResource(id = R.string.disconnected)) {
                     uiEvent = null
                 }
             }
             SyncProgress.NOT_SUPPORTED -> {
                 RuuviMessageDialog(message = stringResource(id = R.string.reading_history_not_supported)) {
                     uiEvent = null
                 }
             }
             SyncProgress.NOT_FOUND -> {
                 RuuviConfirmDialog(
                     title = stringResource(id = R.string.error),
                     message = stringResource(id = R.string.gatt_not_in_range_description),
                     noButtonCaption = stringResource(id = R.string.close),
                     yesButtonCaption = stringResource(id = R.string.try_again),
                     onDismissRequest = {
                         uiEvent = null
                         gattSyncDialogOpened = false
                     }
                 ) {
                     uiEvent = null
                     gattSyncDialogOpened = false
                     viewModel.syncGatt()
                 }
             }
             SyncProgress.ERROR -> {
                 RuuviMessageDialog(message = stringResource(id = R.string.something_went_wrong)) {
                     uiEvent = null
                 }
             }
             else -> {
                 uiEvent = null
             }
         }
     }

    if (gattSyncDialogOpened && !dontShowGattSync) {
        GattSyncDescriptionDialog(
            doNotShowAgain = {
                gattSyncDialogOpened = false
                viewModel.dontShowGattSyncDescription()
            },
            onDismissRequest = {
                gattSyncDialogOpened = false
            }
        )
    }
}

@Composable
fun ViewPeriodMenu(
    viewPeriod: Days,
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
        DropdownMenu(
            modifier = Modifier.background(color = RuuviStationTheme.colors.background),
            expanded = daysMenuExpanded,
            onDismissRequest = { daysMenuExpanded = false }) {
            val daysOptions = listOf(
                Days.Day1(),
                Days.Day2(),
                Days.Day3(),
                Days.Day4(),
                Days.Day5(),
                Days.Day6(),
                Days.Day7(),
                Days.Day8(),
                Days.Day9(),
                Days.Day10(),
            )
            for (day in daysOptions) {
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
        RuuviMessageDialog(
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
    exportToCsv: () -> Uri?,
    clearHistory: () -> Unit,
) {
    val context = LocalContext.current
    var clearConfirmOpened by remember {
        mutableStateOf(false)
    }
    var threeDotsMenuExpanded by remember {
        mutableStateOf(false)
    }

    Box() {
        IconButton(onClick = { threeDotsMenuExpanded = !threeDotsMenuExpanded }) {
            Icon(painter = painterResource(id = R.drawable.ic_3dots),
                contentDescription = null,
                tint = RuuviStationTheme.colors.buttonText
            )
        }

        DropdownMenu(
            modifier = Modifier.background(color = RuuviStationTheme.colors.background),
            expanded = threeDotsMenuExpanded,
            onDismissRequest = { threeDotsMenuExpanded = false }
        ) {
            DropdownMenuItem(onClick = {
                val uri = exportToCsv.invoke()
                if (uri != null) { sendCsv(sensorId, uri, context) }
                threeDotsMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.export_history))
            }
            DropdownMenuItem(onClick = {
                clearConfirmOpened = true
                threeDotsMenuExpanded = false
            }) {
                Paragraph(text = stringResource(id = R.string.clear_view))
            }
        }
    }

    if (clearConfirmOpened) {
        ClearHistoryDialog(
            clearHistory = clearHistory,
            dismissAction = { clearConfirmOpened = false }
        )
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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

fun sendCsv(sensorId: String, uri: Uri, context: Context) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
    sendIntent.type = "text/csv"
    sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_csv_chooser_title, sensorId)))
}