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
import com.ruuvi.station.app.ui.components.PageSurfaceWithPadding
import com.ruuvi.station.app.ui.components.ParagraphWithPadding
import com.ruuvi.station.app.ui.components.RadioButtonRuuvi
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.*

@Composable
fun HumiditySettings(
    scaffoldState: ScaffoldState,
    viewModel: HumiditySettingsViewModel
) {
    val unit = viewModel.humidityUnit.observeAsState(HumidityUnit.Relative)
    val accuracy = viewModel.humidityAccuracy.observeAsState(Accuracy.Accuracy2)

    PageSurfaceWithPadding {
        Column() {
            HumidityUnit(
                allUnits = viewModel.getAllHumidityUnits(),
                selectedUnit = unit,
                onUnitSelected = viewModel::setHumidityUnit
            )
            HumidityAccuracy(
                accuracyList = viewModel.getAccuracyList(),
                accuracy = accuracy,
                selectedUnit = unit,
                onAccuracySelected = viewModel::setHumidityAccuracy
            )
        }
    }
}

@Composable
fun HumidityUnit(
    allUnits: List<HumidityUnit>,
    selectedUnit: State<HumidityUnit>,
    onUnitSelected: (HumidityUnit) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.settings_humidity_unit))

        ParagraphWithPadding(text = stringResource(id = R.string.settings_humidity_unit_details))

        for (item in allUnits) {
            HumidityUnitElement(
                unit = item,
                isSelected = selectedUnit.value == item,
                onUnitSelected = onUnitSelected
            )
        }
    }
}

@Composable
fun HumidityUnitElement(
    unit: HumidityUnit,
    isSelected: Boolean,
    onUnitSelected: (HumidityUnit) -> Unit
) {
    RadioButtonRuuvi(
        text = stringResource(id = unit.unitTitle),
        isSelected = isSelected,
        onClick = { onUnitSelected.invoke(unit) }
    )
}

@Composable
fun HumidityAccuracy(
    accuracyList: Array<Accuracy>,
    accuracy: State<Accuracy>,
    selectedUnit: State<HumidityUnit>,
    onAccuracySelected: (Accuracy) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.humidity_accuracy_title))

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