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
fun PressureSettings(
    scaffoldState: ScaffoldState,
    viewModel: PressureSettingsViewModel
) {
    val unit = viewModel.pressureyUnit.observeAsState(PressureUnit.HectoPascal)
    val accuracy = viewModel.pressureAccuracy.observeAsState(Accuracy.Accuracy2)

    PageSurfaceWithPadding {
        Column() {
            PressureUnit(
                allUnits = viewModel.getAllPressureUnits(),
                selectedUnit = unit,
                onUnitSelected = viewModel::setPressureUnit
            )
            if (unit.value != PressureUnit.Pascal) {
                PressureAccuracy(
                    accuracyList = viewModel.getAccuracyList(),
                    accuracy = accuracy,
                    selectedUnit = unit,
                    onAccuracySelected = viewModel::setPressureAccuracy
                )
            }
        }
    }
}

@Composable
fun PressureUnit(
    allUnits: List<PressureUnit>,
    selectedUnit: State<PressureUnit>,
    onUnitSelected: (PressureUnit) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.settings_pressure_unit))

        ParagraphWithPadding(text = stringResource(id = R.string.settings_pressure_unit_details))

        for (item in allUnits) {
            PressureUnitElement(
                unit = item,
                isSelected = selectedUnit.value == item,
                onUnitSelected = onUnitSelected
            )
        }
    }
}

@Composable
fun PressureUnitElement(
    unit: PressureUnit,
    isSelected: Boolean,
    onUnitSelected: (PressureUnit) -> Unit
) {
    RadioButtonRuuvi(
        text = stringResource(id = unit.unitTitle),
        isSelected = isSelected,
        onClick = { onUnitSelected.invoke(unit) }
    )
}

@Composable
fun PressureAccuracy(
    accuracyList: Array<Accuracy>,
    accuracy: State<Accuracy>,
    selectedUnit: State<PressureUnit>,
    onAccuracySelected: (Accuracy) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = stringResource(id = R.string.pressure_accuracy_title))

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