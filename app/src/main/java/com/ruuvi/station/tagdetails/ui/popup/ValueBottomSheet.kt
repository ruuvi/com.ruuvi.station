package com.ruuvi.station.tagdetails.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.MarkupText
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.modifier.fadingEdge
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.units.domain.score.QualityCalculator
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.getDescriptionBodyResId
import com.ruuvi.station.util.extensions.diffGreaterThan
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.vico.VicoChartNoInteraction
import com.ruuvi.station.vico.model.ChartData
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueBottomSheet (
    sheetValue: EnvironmentValue,
    extraValues: List<EnvironmentValue> = listOf(),
    modifier: Modifier = Modifier,
    chartHistory: ChartData?,
    maxHeight: Int,
    lastUpdate: Date?,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    scrollToChart: (UnitType) -> Unit,
    onChangeValue: (EnvironmentValue) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        containerColor = RuuviStationTheme.colors.popupBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(RuuviStationTheme.colors.popupDragHandle)
            )
        },
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        if (sheetValue.unitType == UnitType.AirQuality.AqiIndex) {
            AirValueSheetContent(
                sheetValue = sheetValue,
                extraValues = extraValues,
                maxHeight = maxHeight,
                lastUpdate = lastUpdate,
                chartHistory = chartHistory,
                scrollToChart = scrollToChart,
                onChangeValue = onChangeValue
            )
        } else {
            ValueSheetContent(
                sheetValue = sheetValue,
                maxHeight = maxHeight,
                chartHistory = chartHistory,
                scrollToChart = scrollToChart,
            )
        }
    }
}

@Composable
fun ValueSheetContent(
    sheetValue: EnvironmentValue,
    maxHeight: Int,
    chartHistory: ChartData?,
    scrollToChart: (UnitType) -> Unit
) {
    val scrollState = rememberScrollState()
    val columnModifier = Modifier.fadingEdge(scrollState)
    Column (
        modifier = columnModifier
            .heightIn(max = maxHeight.pxToDp())
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
            .verticalScroll(scrollState)
    ) {
        ValueSheetHeader(sheetValue)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        if (sheetValue.unitType != UnitType.MovementUnit.MovementsCount && sheetValue.unitType != UnitType.MsnUnit.MsnCount) {
            if (chartHistory != null && chartHistory.segments.isNotEmpty()) {
                VicoChartNoInteraction(
                    chartHistory = chartHistory,
                    modifier = Modifier.clickable {
                        if (sheetValue.unitType != UnitType.MovementUnit.MovementsCount) {
                            scrollToChart(sheetValue.unitType)
                        }
                    }
                )
            } else {
                NoHistoryData()
            }
            if (got2DaysOfHistory(chartHistory)) {
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.small))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                    textAlign = TextAlign.Right,
                    text = stringResource(R.string.day_2),
                )
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }

        MarkupText(sheetValue.unitType.getDescriptionBodyResId())
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
    }
}

fun got2DaysOfHistory(chartHistory: ChartData?): Boolean {
    val firstTimestamp = chartHistory?.segments?.firstOrNull()?.timestamps?.firstOrNull()
    return firstTimestamp?.let {
         Date(it).diffGreaterThan(36*60*60*1000)
    } ?: false
}

@Composable
fun AirValueSheetContent(
    sheetValue: EnvironmentValue,
    extraValues: List<EnvironmentValue> = listOf(),
    maxHeight: Int,
    lastUpdate: Date?,
    chartHistory: ChartData?,
    scrollToChart: (UnitType) -> Unit,
    onChangeValue: (EnvironmentValue) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val columnModifier = Modifier.fadingEdge(scrollState)
    Column(
        modifier = columnModifier
            .heightIn(max = maxHeight.pxToDp())
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
            .verticalScroll(scrollState)
    ) {
        ValueSheetHeader(sheetValue)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))

        if (chartHistory != null && chartHistory.segments.isNotEmpty()) {
            VicoChartNoInteraction(
                chartHistory = chartHistory,
                minMaxLocked = 1.0 to 99.0,
                yAxisValues = listOf(10f, 50f, 80f, 90f, 100f),
                modifier = Modifier.clickable {
                    if (sheetValue.unitType != UnitType.MovementUnit.MovementsCount) {
                        scrollToChart(sheetValue.unitType)
                    }
                }
            )
        } else {
            NoHistoryData()
        }
        if (got2DaysOfHistory(chartHistory)) {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.small))
            Text(
                modifier = Modifier.fillMaxWidth(),
                style = RuuviStationTheme.typography.dashboardSecondary,
                fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                textAlign = TextAlign.Right,
                text = stringResource(R.string.day_2),
            )
        }
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus))
        if (extraValues.isNotEmpty()) {
            for (extra in extraValues) {
                ValueWithIndicator(
                    icon = extra.unitType.iconRes,
                    value = extra.valueWithoutUnit,
                    unit = extra.unitString,
                    name = stringResource(extra.unitType.measurementName),
                    score = QualityCalculator.calc(extra),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    coroutineScope.launch {
                        scrollState.scrollTo(0)
                        onChangeValue.invoke(extra)
                    }
                }
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
            }
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus))
            val advice = remember {
                getBeaverAdvice(context, lastUpdate, sheetValue, extraValues)
            }
            BeaverAdvice(advice)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }
        MarkupText(sheetValue.unitType.getDescriptionBodyResId())
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extraBig))
    }
}

@Composable
fun NoHistoryData() {
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(150.dp)
        , contentAlignment =  Alignment.Center
    ) {
        Text(
            style = RuuviStationTheme.typography.dashboardSecondary,
            fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.popup_no_data),
        )
    }
}


@Preview
@Composable
private fun ValueBottomSheetPreview() {
    val value = EnvironmentValue(
        original = 22.50,
        value = 22.50,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "22.5 %",
        valueWithoutUnit = "22.5",
        unitString = "%",
        unitType = UnitType.HumidityUnit.Relative
    )

    RuuviTheme {
        Surface(color = RuuviStationTheme.colors.popupBackground) {
            ValueSheetContent(
                sheetValue = value,
                maxHeight = 700,
                scrollToChart = {},
                chartHistory = null
            )
        }
    }
}