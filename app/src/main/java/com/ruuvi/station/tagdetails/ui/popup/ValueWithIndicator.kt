package com.ruuvi.station.tagdetails.ui.popup


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.domain.score.QualityRange
import com.ruuvi.station.units.domain.score.ScoreCo2
import com.ruuvi.station.units.model.UnitType

@Composable
fun ValueWithIndicator(
    icon: Int,
    value: String,
    unit: String,
    name: String,
    itemHeight: Dp = RuuviStationTheme.dimensions.sensorCardValueItemHeight,
    score: QualityRange?,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(itemHeight / 2),
        color = RuuviStationTheme.colors.popupButtonBackground,
        //shadowElevation = 1.dp,
        //tonalElevation = 2.dp,
        modifier = Modifier.height(itemHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = modifier
                .clickable { clickAction.invoke() }
        ) {
            Icon(
                modifier = Modifier
                    .height(24.dp.scaleUpTo(1.5f))
                    .padding(horizontal = RuuviStationTheme.dimensions.medium),
                painter = painterResource(id = icon),
                tint = Color(0xff5ebdb2),
                contentDescription = ""
            )

            Column(
                modifier = Modifier
            ) {
                Row() {
                    Text(
                        modifier = Modifier
                            .alignByBaseline(),
                        fontSize = RuuviStationTheme.fontSizes.extended.limitScaleTo(1.5f),
                        style = RuuviStationTheme.typography.dashboardBigValueUnit,
                        fontFamily = ruuviStationFonts.mulishBold,
                        fontWeight = FontWeight.Bold,
                        text = value,
                        color = RuuviStationTheme.colors.popupHeaderText
                    )

                    Text(
                        modifier = Modifier
                            .alignByBaseline()
                            .padding(
                                start = RuuviStationTheme.dimensions.small
                            ),
                        style = RuuviStationTheme.typography.dashboardSecondary,
                        color = RuuviStationTheme.colors.popupHeaderText,
                        fontWeight = FontWeight.Bold,
                        fontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.5f),
                        text = unit,
                        maxLines = 1
                    )
                }
                if (name.isNotEmpty()) {
                    Text(
                        style = RuuviStationTheme.typography.dashboardSecondary,
                        color = RuuviStationTheme.colors.popupHeaderText,
                        fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
                        text = name,
                        maxLines = 1,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            if (score != null) {
                QualityIndicator(
                    color = score.color,
                    description = score.description,
                    modifier = Modifier
                        .padding(end = RuuviStationTheme.dimensions.extended)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ValueWithIndicatorPreview() {
    RuuviTheme {
        val unitType = UnitType.CO2.Ppm
        ValueWithIndicator(
            icon = unitType.iconRes,
            value = "654",
            unit = stringResource(unitType.unit),
            name = stringResource(unitType.measurementName),
            score = ScoreCo2.score(654.0),
            modifier = Modifier.fillMaxWidth(),
        ) {}
    }
}