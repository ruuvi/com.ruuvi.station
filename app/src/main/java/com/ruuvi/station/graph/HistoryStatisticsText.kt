package com.ruuvi.station.graph

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.ruuvi.station.R
import com.ruuvi.station.history.HistoryStatisticsDisplay
import com.ruuvi.station.history.HistoryStatisticsPhase

@Composable
internal fun HistoryStatisticsText(display: HistoryStatisticsDisplay, format: (Double) -> String,
                                   style: TextStyle, modifier: Modifier = Modifier) {
    val stats = display.values
    val values = stringResource(R.string.chart_min_max_avg, stats?.minimum?.let(format) ?: "—",
        stats?.maximum?.let(format) ?: "—", stats?.average?.let(format) ?: "—")
    val measuredHeight = rememberTextMeasurer().measure("Ag", style = style, maxLines = 1).size.height
    val rowHeight = with(LocalDensity.current) { measuredHeight.toDp() }
    if (stats == null && display.phase != HistoryStatisticsPhase.READY) {
        // No placeholder text or accessibility announcement before the first
        // readings. Keep the same row height so the plot never shifts.
        Spacer(modifier.height(rowHeight))
        return
    }
    BasicText(values, modifier.height(rowHeight),
        style = style.copy(color = Color.White.copy(alpha = if (display.phase == HistoryStatisticsPhase.READY) .5f else .38f)),
        maxLines = 1, softWrap = false, autoSize = TextAutoSize.StepBased(9.sp, style.fontSize))
}
