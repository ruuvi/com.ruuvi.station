package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.MarkupText
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.getDescriptionBodyResId
import com.ruuvi.station.util.ui.pxToDp
import com.ruuvi.station.vico.VicoChartNoInteraction
import com.ruuvi.station.vico.model.ChartData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ValueBottomSheet (
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier,
    chartHistory: ChartData?,
    maxHeight: Int,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        containerColor = RuuviStationTheme.colors.sensorValueBottomSheetBackground,
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.7f))
            )
        },
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        ValueSheetContent(
            sheetValue = sheetValue,
            maxHeight = maxHeight,
            chartHistory = chartHistory
        )
    }
}

@Composable
fun ValueSheetContent(
    sheetValue: EnvironmentValue,
    maxHeight: Int,
    chartHistory: ChartData?
) {

    Column (
        modifier = Modifier
            .heightIn(max = maxHeight.pxToDp())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
    ) {
        ValueSheetHeader(sheetValue)
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        if (chartHistory != null && chartHistory.timestamps.isNotEmpty()) {
            VicoChartNoInteraction(chartHistory)
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
        }
        MarkupText(sheetValue.unitType.getDescriptionBodyResId())
        Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.extended))
    }
}

@Composable
fun ValueSheetHeader(
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {

        Icon(
            modifier = Modifier
                .height(24.dp.scaleUpTo(1.5f))
                .padding(end = RuuviStationTheme.dimensions.medium),
            painter = painterResource(id = sheetValue.unitType.iconRes),
            tint = Color(0xff5ebdb2),
            contentDescription = ""
        )

        ValueSheetHeaderText(
            modifier = Modifier
                .alignByBaseline(),
            text = stringResource(sheetValue.unitType.measurementTitle)
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ValueSheetHeaderText(
                modifier = Modifier
                    .alignByBaseline(),
                text = sheetValue.valueWithoutUnit
            )

            ValueSheetUnitText(
                modifier = Modifier
                    .alignByBaseline()
                    .padding(
                        start = RuuviStationTheme.dimensions.small
                    ),
                text = sheetValue.unitString
            )
        }
    }
}

@Composable
fun ValueSheetHeaderText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        fontSize = RuuviStationTheme.fontSizes.normal.limitScaleTo(1.5f),
        fontFamily = ruuviStationFonts.mulishBold,
        text = text,
        color = Color.White
    )
}

@Composable
fun ValueSheetUnitText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        style = RuuviStationTheme.typography.dashboardSecondary,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
        text = text,
        maxLines = 1
    )
}

@Preview
@Composable
private fun ValueBottomSheet() {
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
        Surface(color = RuuviStationTheme.colors.sensorValueBottomSheetBackground) {
            ValueSheetContent(
                sheetValue = value,
                maxHeight = 700,
                chartHistory = null
            )
        }
    }
}