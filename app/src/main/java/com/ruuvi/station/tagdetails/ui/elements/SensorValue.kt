package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.White80
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.model.UnitType

@Composable
fun SensorValueItem(
    icon: Int,
    value: String,
    unit: String,
    name: String,
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit
) {
    val internalModifier = Modifier
        .height(itemHeight)
        .clip(RoundedCornerShape(itemHeight / 2))
        .background(Color.White.copy(alpha = 0.1f))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .then(internalModifier)
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

        Column (
            modifier = Modifier.padding(
                end = RuuviStationTheme.dimensions.extended
            )
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
                    color = Color.White
                )

                Text(
                    modifier = Modifier
                        .alignByBaseline()
                        .padding(
                            start = RuuviStationTheme.dimensions.small
                        ),
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.5f),
                    text = unit,
                    maxLines = 1
                )
            }
            if (name.isNotEmpty()) {
                Text(
                    style = RuuviStationTheme.typography.dashboardSecondary,
                    color = White80,
                    fontSize = RuuviStationTheme.fontSizes.miniature.limitScaleTo(1.5f),
                    text = name,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun SensorValueName(
    icon: Int,
    name: String,
    itemHeight: Dp,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit
) {
    val internalModifier = Modifier
        .height(itemHeight)
        .clip(RoundedCornerShape(itemHeight / 2))
        .background(Color.White.copy(alpha = 0.1f))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = internalModifier
            .then(modifier)
            .clickable { clickAction.invoke() }
    ) {
        Icon(
            modifier = Modifier
                .height(24.dp)
                .padding(horizontal = RuuviStationTheme.dimensions.medium),
            painter = painterResource(id = icon),
            tint = Color(0xff5ebdb2),
            contentDescription = ""
        )
        Text(
            modifier = Modifier
                .padding(end = RuuviStationTheme.dimensions.extended),
            fontSize = RuuviStationTheme.fontSizes.compact.limitScaleTo(1.5f),
            style = RuuviStationTheme.typography.dashboardSecondary,
            fontFamily = ruuviStationFonts.mulishBold,
            fontWeight = FontWeight.Bold,
            text = name,
            color = Color.White
        )
    }
}

@Preview
@Composable
private fun SensorValueItemPreview() {
    RuuviTheme {
        val unitType = UnitType.TemperatureUnit.Celsius
        SensorValueItem(
            icon = unitType.iconRes,
            value = "23.5",
            unit = stringResource(unitType.unit),
            name = stringResource(unitType.measurementTitle),
            itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight,
            modifier = Modifier,
        ) {}
    }
}

@Preview
@Composable
private fun SensorValueNamePreview() {
    RuuviTheme {
        val unitType = UnitType.HumidityUnit.Relative
        SensorValueName(
            icon = unitType.iconRes,
            name = stringResource(unitType.measurementTitle),
            itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight,
            modifier = Modifier,
        ) {}
    }
}