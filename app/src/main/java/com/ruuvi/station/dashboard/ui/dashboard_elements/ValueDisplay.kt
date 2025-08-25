package com.ruuvi.station.dashboard.ui.dashboard_elements

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ruuvi.station.app.ui.components.limitScaleTo
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType

@Composable
fun ValueDisplay(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier,
    extended: Boolean = false
) {
    if (extended) {
        ValueDisplayExtended(
            value = value,
            alertTriggered = alertTriggered,
            modifier = modifier
        )
    } else {
        ValueDisplaySimple(
            value = value,
            alertTriggered = alertTriggered,
            modifier = modifier
        )
    }
}

@Composable
fun ValueDisplayExtended(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        RuuviStationTheme.colors.dashboardValue
    }

    val unit = if (value.unitType.extraUnit != null) {
        val extraUnit = stringResource(value.unitType.extraUnit)
        "$extraUnit, ${value.unitString}"
    } else {
        value.unitString
    }

    Column {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                modifier = Modifier.alignByBaseline(),
                text = value.valueWithoutUnit,
                style = RuuviStationTheme.typography.dashboardValue,
                fontSize = ruuviStationFontsSizes.compact.limitScaleTo(1.5f),
                color = textColor,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(width = 4.dp))
            Text(
                modifier = Modifier.alignByBaseline(),
                text = unit,
                style = RuuviStationTheme.typography.dashboardUnit,
                fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
                maxLines = 1
            )
        }
        Text(
            modifier = Modifier,
            text = stringResource(value.unitType.measurementTitle),
            style = RuuviStationTheme.typography.dashboardValueTitle,
            fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
            maxLines = 1
        )
    }
}

@Composable
fun ValueDisplaySimple(
    value: EnvironmentValue,
    alertTriggered: Boolean,
    modifier: Modifier = Modifier
) {
    val textColor = if (alertTriggered) {
        RuuviStationTheme.colors.activeAlertThemed
    } else {
        RuuviStationTheme.colors.primary
    }

    val unit = if (value.unitType.extraUnit != null) {
        val extraUnit = stringResource(value.unitType.extraUnit)
        "$extraUnit, ${value.unitString}"
    } else {
        value.unitString
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            modifier = Modifier.alignByBaseline(),
            text = value.valueWithoutUnit,
            style = RuuviStationTheme.typography.dashboardValue,
            fontSize = ruuviStationFontsSizes.compact.limitScaleTo(1.5f),
            color = textColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(width = 4.dp))
        Text(
            modifier = Modifier.alignByBaseline(),
            text = unit,
            style = RuuviStationTheme.typography.dashboardUnit,
            fontSize = ruuviStationFontsSizes.petite.limitScaleTo(1.5f),
            color = textColor,
            maxLines = 1
        )
    }
}

@Preview
@Composable
private fun ValueDisplayPreview() {
    RuuviTheme {
        ValueDisplay(
            value = EnvironmentValue(
                original = 22.50,
                value = 22.50,
                accuracy = Accuracy.Accuracy1,
                valueWithUnit = "22.5 %",
                valueWithoutUnit = "22.5",
                unitString = "%",
                unitType = UnitType.HumidityUnit.Relative
            ),
            alertTriggered = false,
        )
    }
}