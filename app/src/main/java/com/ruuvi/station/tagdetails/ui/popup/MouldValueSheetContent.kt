package com.ruuvi.station.tagdetails.ui.popup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.MarkupText
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.units.domain.score.QualityRange
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.vico.VicoChartNoInteraction
import com.ruuvi.station.vico.model.ChartData

@Composable
fun MouldValueSheetContent(
    value: EnvironmentValue,
    inputs: List<EnvironmentValue>,
    maxHeight: Int,
    chartHistory: ChartData?,
    scrollToChart: (UnitType) -> Unit,
    onChangeValue: (EnvironmentValue) -> Unit
) {
    Column(
        Modifier.heightIn(max = maxHeight.pxToDp())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
    ) {
        ValueSheetHeader(value)
        Spacer(Modifier.height(RuuviStationTheme.dimensions.medium))
        if (!value.isAvailable) {
            Text(
                stringResource(value.unavailableReason ?: R.string.mould_risk_unavailable),
                color = RuuviStationTheme.colors.popupHeaderText
            )
            Spacer(Modifier.height(RuuviStationTheme.dimensions.medium))
        }
        Text(stringResource(R.string.mould_risk_scale), color = RuuviStationTheme.colors.popupHeaderText)
        Spacer(Modifier.height(RuuviStationTheme.dimensions.medium))
        if (chartHistory != null && chartHistory.segments.isNotEmpty()) {
            VicoChartNoInteraction(
                chartHistory = chartHistory,
                minMaxLocked = 0.0 to 100.0,
                rangePadding = 0.0,
                yAxisValues = listOf(0f, 25f, 50f, 75f, 100f),
                modifier = Modifier.clickable { scrollToChart(UnitType.MouldRisk.Index) }
            )
        } else {
            NoHistoryData()
        }
        Spacer(Modifier.height(RuuviStationTheme.dimensions.medium))
        ValueSheetHeaderText(stringResource(R.string.mould_risk_inputs))
        inputs.forEach { input ->
            Spacer(Modifier.height(RuuviStationTheme.dimensions.small))
            ValueWithIndicator(
                icon = input.unitType.iconRes,
                value = input.valueWithoutUnit,
                unit = input.unitString,
                name = stringResource(input.unitType.measurementName),
                score = null,
                modifier = Modifier.fillMaxWidth()
            ) { onChangeValue(input) }
        }
        Spacer(Modifier.height(RuuviStationTheme.dimensions.extended))
        val bands = listOf(
            "0–9" to QualityRange.MouldVeryLow,
            "10–24" to QualityRange.MouldLow,
            "25–49" to QualityRange.MouldElevated,
            "50–74" to QualityRange.MouldHigh,
            "75–100" to QualityRange.MouldVeryHigh
        )
        bands.forEach { (range, quality) ->
            // Keep readable text in the theme colour; the swatch carries the band colour.
            androidx.compose.foundation.layout.Row {
                Text("●  ", color = quality.color)
                Text(
                    stringResource(R.string.mould_risk_legend_item, range, stringResource(quality.description)),
                    color = RuuviStationTheme.colors.popupHeaderText
                )
            }
            Spacer(Modifier.height(RuuviStationTheme.dimensions.small))
        }
        Spacer(Modifier.height(RuuviStationTheme.dimensions.extended))
        MarkupText(R.string.description_text_mould_risk)
        Spacer(Modifier.height(RuuviStationTheme.dimensions.extraBig))
    }
}
