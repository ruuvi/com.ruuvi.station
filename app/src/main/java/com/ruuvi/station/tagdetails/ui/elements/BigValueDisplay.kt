package com.ruuvi.station.tagdetails.ui.elements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFonts
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BigValueDisplay(
    value: EnvironmentValue,
    showName: Boolean,
    modifier: Modifier = Modifier
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row(
            modifier = modifier.wrapContentSize(),
            verticalAlignment = Top
        ) {
            Text(
                modifier = Modifier
                    .offset(y = 14.dp)
                    .padding(start = 4.dp),
                fontSize = 36.sp.limitScaleTo(1.2f),
                fontFamily = ruuviStationFonts.oswaldRegular,
                text = value.unitString,
                color = Color.Transparent
            )
            Text(
                modifier = Modifier,
                fontSize = 72.sp.limitScaleTo(1.5f),
                fontFamily = ruuviStationFonts.oswaldBold,
                text = value.valueWithoutUnit,
                color = Color.White
            )

            Text(
                modifier = Modifier
                    .offset(y = 14.dp)
                    .padding(start = 4.dp),
                fontSize = 36.sp.limitScaleTo(1.2f),
                fontFamily = ruuviStationFonts.oswaldRegular,
                text = value.unitString,
                color = Color.White
            )
        }
        if (showName) {
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))

            SensorValueName(
                icon = value.unitType.iconRes,
                name = stringResource(value.unitType.measurementTitle),
                itemHeight = RuuviStationTheme.dimensions.sensorCardValueItemHeight,
                alertActive = true,
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.extended)
            ) {
                showBottomSheet = true
            }
        }
    }

    if (showBottomSheet) {
        ValueBottomSheet(
            sheetValue = value,
            modifier = Modifier
        ) {
            showBottomSheet = false
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
            modifier = Modifier
        )
    }
}

