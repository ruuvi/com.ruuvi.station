package com.ruuvi.station.graph

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import java.text.NumberFormat

/** One fixed toolbar slot for idle, Bluetooth and cloud states. Never changes the chart's bounds. */
@Composable
internal fun HistorySyncControl(
    bluetoothSyncing: Boolean,
    bluetoothMessage: String,
    cloudProgress: Float?,
    sync: () -> Unit,
    cancelBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = cloudProgress?.takeIf { it.isFinite() }?.coerceIn(0f, 1f)
    val locale = LocalConfiguration.current.locales[0]
    val percentage = remember(progress, locale) {
        progress?.let { NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = 0 }.format(it) }
    }
    val text = when {
        bluetoothSyncing -> bluetoothMessage
        percentage != null -> stringResource(R.string.history_downloading_progress, percentage)
        else -> stringResource(R.string.sync)
    }
    val typography = RuuviStationTheme.typography
    val style = if (bluetoothSyncing) typography.syncStatusText else typography.subtitle
    Row(modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        if (bluetoothSyncing) {
            IconButton(onClick = cancelBluetooth) {
                Icon(painterResource(R.drawable.ic_baseline_clear_24), stringResource(R.string.cancel),
                    tint = RuuviStationTheme.colors.buttonText)
            }
            BasicText(text, Modifier.weight(1f).padding(end = 8.dp),
                style = style, maxLines = 1, softWrap = false,
                autoSize = TextAutoSize.StepBased(10.sp, style.fontSize))
        } else {
            Row(Modifier.fillMaxSize().clickable(enabled = progress == null, role = Role.Button, onClick = sync)
                .padding(start = RuuviStationTheme.dimensions.extended, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.gatt_sync), null, tint = RuuviStationTheme.colors.buttonText)
                Spacer(Modifier.width(RuuviStationTheme.dimensions.medium))
                BasicText(text, Modifier.weight(1f),
                    style = style.copy(color = RuuviStationTheme.colors.buttonText),
                    maxLines = 1, softWrap = false,
                    autoSize = TextAutoSize.StepBased(10.sp, style.fontSize))
            }
        }
    }
}
