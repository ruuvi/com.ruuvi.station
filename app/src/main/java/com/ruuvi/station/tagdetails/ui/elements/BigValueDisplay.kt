package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.fixedSp
import com.ruuvi.station.app.ui.components.scaleUpTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@Composable
fun BigValueDisplay(
    value: EnvironmentValue,
    showName: Boolean,
    alertActive: Boolean,
    modifier: Modifier = Modifier,
    clickAction: () -> Unit = {}
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Box(
            contentAlignment = Center,
            modifier = Modifier.height(140.dp)
        ) {
            Row(
                modifier = modifier.wrapContentSize(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    modifier = Modifier
                        .padding(start = 4.dp),
                    fontSize = RuuviStationTheme.fontSizes.big.fixedSp(),
                    fontFamily = ruuviStationFonts.oswaldRegular,
                    text = value.unitString,
                    color = Color.Transparent
                )
                Text(
                    modifier = Modifier
                        .padding(bottom = 8.dp),
                    fontSize = RuuviStationTheme.fontSizes.bigValue.fixedSp(),
                    fontFamily = ruuviStationFonts.oswaldBold,
                    text = value.valueWithoutUnit,
                    color = Color.White
                )
                Text(
                    modifier = Modifier
                        .offset(y = 14.dp)
                        .padding(start = 4.dp),
                    fontSize = RuuviStationTheme.fontSizes.big.fixedSp(),
                    fontFamily = ruuviStationFonts.oswaldRegular,
                    text = value.unitString,
                    color = Color.White
                )
            }
        }
        if (showName) {
            SensorUnitName(
                icon = value.unitType.iconRes,
                name = stringResource(value.unitType.measurementName),
                itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight.scaleUpTo(1.5f),
                alertActive = alertActive,
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended)
            ) {
                clickAction.invoke()
            }
        }
    }
}

@Preview
@Composable
private fun BigValueDisplayPreview() {
    RuuviTheme {
        BigValueDisplay(
            value = EnvironmentValue(
                original = 22.50,
                value = 22.50,
                accuracy = Accuracy.Accuracy1,
                valueWithUnit = "22.5 %",
                valueWithoutUnit = "22.5",
                unitString = "%",
                unitType = UnitType.HumidityUnit.Relative
            ),
            showName = true,
            alertActive = false,
            modifier = Modifier
        )
    }
}

