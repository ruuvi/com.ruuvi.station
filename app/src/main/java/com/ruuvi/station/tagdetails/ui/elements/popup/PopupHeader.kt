package com.ruuvi.station.tagdetails.ui.elements.popup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.domain.score.ScoreAqi
import com.ruuvi.station.units.domain.score.ScoreCo2
import com.ruuvi.station.units.domain.score.ScorePM
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@Composable
fun ValueSheetHeader(
    sheetValue: EnvironmentValue,
    modifier: Modifier = Modifier
) {
    val score = when (sheetValue.unitType) {
        is UnitType.AirQuality.AqiIndex -> ScoreAqi.score(sheetValue.value)
        is UnitType.CO2.Ppm -> ScoreCo2.score(sheetValue.value)
        is UnitType.PM.PM25 -> ScorePM.score(sheetValue.value)
        else -> null
    }

    Column (
        verticalArrangement = Arrangement.Top
    ){

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
                modifier = Modifier,
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
        if (score != null) {
            QualityIndicator(
                color = score.color,
                description = score.description,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(vertical = 4.dp)
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
        color = RuuviStationTheme.colors.popupHeaderText
    )
}

@Composable
fun ValueSheetUnitText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        fontFamily = ruuviStationFonts.mulishBold,
        color = RuuviStationTheme.colors.popupHeaderText,
        fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
        text = text,
        maxLines = 1
    )
}

@PreviewLightDark
@Composable
private fun HeaderPreview() {
    val value = EnvironmentValue(
        original = 22.50,
        value = 22.50,
        accuracy = Accuracy.Accuracy1,
        valueWithUnit = "22.5 %",
        valueWithoutUnit = "22.5",
        unitString = "%",
        unitType = UnitType.AirQuality.AqiIndex
    )

    RuuviTheme {
        Surface(color = RuuviStationTheme.colors.popupBackground) {
            ValueSheetHeader(
                sheetValue = value
            )
        }
    }
}