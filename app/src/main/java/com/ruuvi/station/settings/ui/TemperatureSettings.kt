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
import com.ruuvi.station.units.model.UnitType.*

@Composable
fun TemperatureSettings(
    scaffoldState: ScaffoldState,
    viewModel: TemperatureSettingsViewModel
) {
    val unit = viewModel.temperatureUnit.observeAsState(TemperatureUnit.Celsius)
    val accuracy = viewModel.temperatureAccuracy.observeAsState(Accuracy.Accuracy2)
    PageSurfaceWithPadding {
        Column() {
            TemperatureUnit(
                allUnits = viewModel.getAllTemperatureUnits(),
                selectedUnit = unit,
                onUnitSelected = viewModel::setTemperatureUnit
            )
            TemperatureAccuracy(
                accuracyList = viewModel.getAccuracyList(),
                accuracy = accuracy,
                selectedUnit = unit,
                onAccuracySelected = viewModel::setTemperatureAccuracy
            )
        }
    }
}

@Composable
fun TemperatureUnit(
    allUnits: List<TemperatureUnit>,
    selectedUnit: State<TemperatureUnit>,
    onUnitSelected: (TemperatureUnit) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.settings_temperature_unit))

        ParagraphWithPadding(text = stringResource(id = R.string.settings_temperature_unit_details))

        for (item in allUnits) {
            TemperatureUnitElement(
                unit = item,
                selectedUnit.value == item,
                onUnitSelected = onUnitSelected
            )
        }
    }
}

@Composable
fun TemperatureUnitElement(
    unit: TemperatureUnit,
    isSelected: Boolean,
    onUnitSelected: (TemperatureUnit) -> Unit
) {
    RadioButtonRuuvi(
        text = unit.unitTitle?.let { stringResource(id = R.string.empty) } ?: "",
        isSelected = isSelected,
        onClick = { onUnitSelected.invoke(unit) }
    )
}

@Composable
fun TemperatureAccuracy(
    accuracyList: Array<Accuracy>,
    accuracy: State<Accuracy>,
    selectedUnit: State<TemperatureUnit>,
    onAccuracySelected: (Accuracy) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.temperature_accuracy_title))

        ParagraphWithPadding(text = stringResource(id = R.string.accuracy_description))

        for (item in accuracyList) {
            AccuracyElement(
                accuracy = item,
                isSelected = accuracy.value == item,
                unit = stringResource(id = selectedUnit.value.unit),
                onAccuracySelected
            )
        }
    }
}

@Composable
fun AccuracyElement(
    accuracy: Accuracy,
    isSelected: Boolean,
    unit: String,
    onAccuracyChange: (Accuracy) -> Unit
) {
    RadioButtonRuuvi(
        text = stringResource(id = accuracy.nameTemplateId, accuracy.value, unit),
        isSelected = isSelected,
        onClick = {onAccuracyChange.invoke(accuracy)}
    )
}