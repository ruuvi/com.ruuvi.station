package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.TemperatureUnit

@Composable
fun TemperatureSettings(
    scaffoldState: ScaffoldState,
    viewModel: TemperatureSettingsViewModel
) {
    val temperatureUnit = viewModel.temperatureUnit.observeAsState(TemperatureUnit.CELSIUS)
    val temperatureAccuracy = viewModel.temperatureAccuracy.observeAsState(Accuracy.Accuracy2)
    PageSurfaceWithPadding {
        Column() {
            TemperatureUnit(
                temperatureUnits = viewModel.getAllTemperatureUnits(),
                selectedItem = temperatureUnit,
                onUnitSelected = viewModel::setTemperatureUnit
            )
            TemperatureAccuracy(
                accuracyList = viewModel.getAccuracyList(),
                temperatureAccuracy = temperatureAccuracy,
                temperatureUnit = temperatureUnit,
                onAccuracySelected = viewModel::setTemperatureAccuracy
            )
        }
    }
}

@Composable
fun TemperatureUnit(
    temperatureUnits: Array<TemperatureUnit>,
    selectedItem: State<TemperatureUnit>,
    onUnitSelected: (TemperatureUnit) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.settings_temperature_unit))

        ParagraphWithPadding(text = stringResource(id = R.string.settings_temperature_unit_details))

        for (item in temperatureUnits) {
            TemperatureUnitElement(
                temperatureUnit = item,
                selectedItem.value == item,
                onUnitSelected = onUnitSelected
            )
        }
    }
}

@Composable
fun TemperatureUnitElement(
    temperatureUnit: TemperatureUnit,
    isSelected: Boolean,
    onUnitSelected: (TemperatureUnit) -> Unit
) {
    RadioButtonRuuvi(
        text = stringResource(id = temperatureUnit.title),
        isSelected = isSelected,
        onClick = { onUnitSelected.invoke(temperatureUnit) }
    )
}

@Composable
fun TemperatureAccuracy(
    accuracyList: Array<Accuracy>,
    temperatureAccuracy: State<Accuracy>,
    temperatureUnit: State<TemperatureUnit>,
    onAccuracySelected: (Accuracy) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.temperature_accuracy_title))

        ParagraphWithPadding(text = stringResource(id = R.string.accuracy_description))

        for (item in accuracyList) {
            AccuracyElement(
                accuracy = item,
                isSelected = temperatureAccuracy.value == item,
                temperatureUnit = temperatureUnit,
                onAccuracySelected
            )
        }
    }
}

@Composable
fun AccuracyElement(
    accuracy: Accuracy,
    isSelected: Boolean,
    temperatureUnit: State<TemperatureUnit>,
    onAccuracyChange: (Accuracy) -> Unit
) {
    RadioButtonRuuvi(
        text = stringResource(id = accuracy.nameTemplateId, accuracy.value, stringResource(id = temperatureUnit.value.unit)),
        isSelected = isSelected,
        onClick = {onAccuracyChange.invoke(accuracy)}
    )
}