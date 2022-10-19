package com.ruuvi.station.graph

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.RuuviConfirmDialog
import com.ruuvi.station.app.ui.components.RuuviMessageDialog
import com.ruuvi.station.app.ui.components.Subtitle
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
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

    val syncMessage by viewModel.gattSyncStatus.collectAsState(UiText.EmptyString())

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (canUseGatt && !syncInProgress) {
            IconButton(onClick = {
                viewModel.syncGatt()
            }) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_widget_d_update),
                    contentDescription = null,
                    tint = RuuviStationTheme.colors.buttonText
                )
            }
        }
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
            Paragraph(text = syncMessage.asString(context))
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
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(key1 = true) {
        viewModel.event.collect() { event ->
            if (event is UiEvent.ShowPopup) {
                uiEvent = event.message.asString(context)
            }
        }
    }
     if (uiEvent != null) {
         RuuviMessageDialog(message = uiEvent ?: "") {
            uiEvent = null
         }
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
            Subtitle(text = daysText)

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
            Icon(painter = painterResource(id = R.drawable.ic_menu_24),
                contentDescription = null,
                tint = RuuviStationTheme.colors.buttonText
            )
        }

        DropdownMenu(modifier = Modifier.background(color = RuuviStationTheme.colors.background), expanded = threeDotsMenuExpanded, onDismissRequest = { threeDotsMenuExpanded = false }) {
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
        RuuviConfirmDialog(
            message = stringResource(id = R.string.clear_confirm),
            onDismissRequest = { clearConfirmOpened = false }
        ) {
            clearHistory.invoke()
            clearConfirmOpened = false
        }
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