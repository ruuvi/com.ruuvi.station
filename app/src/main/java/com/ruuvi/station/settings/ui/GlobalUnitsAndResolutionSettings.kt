package com.ruuvi.station.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.PageSurface
import com.ruuvi.station.app.ui.components.PageSurfaceWithPadding
import com.ruuvi.station.app.ui.components.ParagraphWithPadding
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.settings.domain.GlobalUnitsSettingsType
import com.ruuvi.station.settings.domain.ResolutionSettingsTarget
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import com.ruuvi.station.units.model.UnitType.PressureUnit
import com.ruuvi.station.units.model.UnitType.TemperatureUnit

@Composable
fun GlobalUnitsSettings(
    scaffoldState: ScaffoldState,
    onNavigate: (String) -> Unit,
    viewModel: GlobalUnitsAndResolutionViewModel
) {
    val temperatureUnit = viewModel.temperatureUnit.observeAsState(TemperatureUnit.Celsius)
    val humidityUnit = viewModel.humidityUnit.observeAsState(HumidityUnit.Relative)
    val pressureUnit = viewModel.pressureUnit.observeAsState(PressureUnit.HectoPascal)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PageSurface {
        Column(modifier = Modifier.fillMaxWidth()) {
            ParagraphWithPadding(
                text = stringResource(id = R.string.settings_global_units_description),
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
            )

            SettingsElement(
                name = stringResource(id = R.string.temperature),
                value = stringResource(id = temperatureUnit.value.unitTitle),
                onClick = {
                    onNavigate.invoke(
                        SettingsRoutes.globalUnitSelectRoute(
                            GlobalUnitsSettingsType.Temperature.routeCode
                        )
                    )
                }
            )

            SettingsElement(
                name = stringResource(id = R.string.humidity),
                value = stringResource(id = humidityUnit.value.unitTitle),
                onClick = {
                    onNavigate.invoke(
                        SettingsRoutes.globalUnitSelectRoute(
                            GlobalUnitsSettingsType.Humidity.routeCode
                        )
                    )
                }
            )

            SettingsElement(
                name = stringResource(id = R.string.pressure),
                value = stringResource(id = pressureUnit.value.unitTitle),
                onClick = {
                    onNavigate.invoke(
                        SettingsRoutes.globalUnitSelectRoute(
                            GlobalUnitsSettingsType.Pressure.routeCode
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun GlobalUnitSelectionSettings(
    scaffoldState: ScaffoldState,
    unitType: String?,
    viewModel: GlobalUnitsAndResolutionViewModel
) {
    val selectedTemperatureUnit = viewModel.temperatureUnit.observeAsState(TemperatureUnit.Celsius)
    val selectedHumidityUnit = viewModel.humidityUnit.observeAsState(HumidityUnit.Relative)
    val selectedPressureUnit = viewModel.pressureUnit.observeAsState(PressureUnit.HectoPascal)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PageSurfaceWithPadding {
        when (GlobalUnitsSettingsType.getByRouteCode(unitType)) {
            GlobalUnitsSettingsType.Temperature -> TemperatureUnit(
                allUnits = viewModel.getAllTemperatureUnits(),
                selectedUnit = selectedTemperatureUnit,
                onUnitSelected = viewModel::setTemperatureUnit
            )

            GlobalUnitsSettingsType.Humidity -> HumidityUnit(
                allUnits = viewModel.getAllHumidityUnits(),
                selectedUnit = selectedHumidityUnit,
                onUnitSelected = viewModel::setHumidityUnit
            )

            GlobalUnitsSettingsType.Pressure -> PressureUnit(
                allUnits = viewModel.getAllPressureUnits(),
                selectedUnit = selectedPressureUnit,
                onUnitSelected = viewModel::setPressureUnit
            )

            null -> {}
        }
    }
}

@Composable
fun ResolutionSettings(
    scaffoldState: ScaffoldState,
    onNavigate: (String) -> Unit,
    viewModel: GlobalUnitsAndResolutionViewModel
) {
    val temperatureUnit = viewModel.temperatureUnit.observeAsState(TemperatureUnit.Celsius)
    val pressureUnit = viewModel.pressureUnit.observeAsState(PressureUnit.HectoPascal)
    val accuracyValues = viewModel.accuracyValues.observeAsState(emptyMap())
    val resolutionTargets = viewModel.getResolutionTargets()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PageSurface {
        Column(modifier = Modifier.fillMaxWidth()) {
            ParagraphWithPadding(
                text = stringResource(
                    id = if (resolutionTargets.isEmpty()) {
                        R.string.settings_resolution_empty
                    } else {
                        R.string.accuracy_description
                    }
                ),
                modifier = Modifier.padding(horizontal = RuuviStationTheme.dimensions.screenPadding)
            )

            for (target in resolutionTargets) {
                val enabled = isResolutionTargetEnabled(
                    target = target,
                    pressureUnit = pressureUnit.value
                )
                SettingsElement(
                    name = stringResource(id = target.titleRes()),
                    value = accuracyText(
                        accuracy = target.accuracy(
                            accuracyValues = accuracyValues.value,
                            pressureUnit = pressureUnit.value
                        ),
                        unit = target.unitString(
                            temperatureUnit = temperatureUnit.value,
                            pressureUnit = pressureUnit.value
                        )
                    ),
                    enabled = enabled,
                    onClick = {
                        onNavigate.invoke(SettingsRoutes.resolutionSelectRoute(target.routeCode))
                    }
                )
            }
        }
    }
}

@Composable
fun ResolutionSelectionSettings(
    scaffoldState: ScaffoldState,
    target: String?,
    viewModel: GlobalUnitsAndResolutionViewModel
) {
    val temperatureUnit = viewModel.temperatureUnit.observeAsState(TemperatureUnit.Celsius)
    val pressureUnit = viewModel.pressureUnit.observeAsState(PressureUnit.HectoPascal)
    val accuracyValues = viewModel.accuracyValues.observeAsState(emptyMap())
    val resolutionTarget = ResolutionSettingsTarget.getByRouteCode(target)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    PageSurfaceWithPadding {
        if (resolutionTarget != null && isResolutionTargetEnabled(resolutionTarget, pressureUnit.value)) {
            ResolutionAccuracy(
                title = stringResource(id = resolutionTarget.titleRes()),
                accuracyList = resolutionTarget.availableAccuracies(),
                accuracy = resolutionTarget.accuracy(
                    accuracyValues = accuracyValues.value,
                    pressureUnit = pressureUnit.value
                ),
                unit = resolutionTarget.unitString(
                    temperatureUnit = temperatureUnit.value,
                    pressureUnit = pressureUnit.value
                ),
                onAccuracySelected = { viewModel.setAccuracy(resolutionTarget, it) }
            )
        }
    }
}

@Composable
fun ResolutionAccuracy(
    title: String,
    accuracyList: List<Accuracy>,
    accuracy: Accuracy,
    unit: String,
    onAccuracySelected: (Accuracy) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SubtitleWithPadding(text = title)

        ParagraphWithPadding(text = stringResource(id = R.string.accuracy_description))

        for (item in accuracyList) {
            AccuracyElement(
                accuracy = item,
                isSelected = accuracy == item,
                unit = unit,
                onAccuracyChange = onAccuracySelected
            )
        }
    }
}

@Composable
private fun accuracyText(
    accuracy: Accuracy,
    unit: String
): String =
    stringResource(id = accuracy.nameTemplateId, accuracy.value, unit)

private fun ResolutionSettingsTarget.titleRes(): Int =
    when (this) {
        ResolutionSettingsTarget.Temperature -> R.string.temperature
        ResolutionSettingsTarget.RelativeHumidity -> R.string.relative_humidity
        ResolutionSettingsTarget.AbsoluteHumidity -> R.string.absolute_humidity
        ResolutionSettingsTarget.DewPoint -> R.string.dewpoint
        ResolutionSettingsTarget.Pressure -> R.string.pressure
        ResolutionSettingsTarget.ParticulateMatter -> R.string.pm
        ResolutionSettingsTarget.Acceleration -> R.string.acceleration
        ResolutionSettingsTarget.Voltage -> R.string.battery_voltage
    }

@Composable
private fun ResolutionSettingsTarget.unitString(
    temperatureUnit: TemperatureUnit,
    pressureUnit: PressureUnit
): String =
    when (this) {
        ResolutionSettingsTarget.Temperature -> stringResource(id = temperatureUnit.unit)
        ResolutionSettingsTarget.RelativeHumidity -> stringResource(id = HumidityUnit.Relative.unit)
        ResolutionSettingsTarget.AbsoluteHumidity -> stringResource(id = HumidityUnit.Absolute.unit)
        ResolutionSettingsTarget.DewPoint -> stringResource(id = temperatureUnit.unit)
        ResolutionSettingsTarget.Pressure -> stringResource(id = pressureUnit.unit)
        ResolutionSettingsTarget.ParticulateMatter -> stringResource(id = R.string.unit_pm25)
        ResolutionSettingsTarget.Acceleration -> stringResource(id = R.string.acceleration_unit)
        ResolutionSettingsTarget.Voltage -> stringResource(id = R.string.voltage_unit)
    }

private fun ResolutionSettingsTarget.accuracy(
    accuracyValues: Map<ResolutionSettingsTarget, Accuracy>,
    pressureUnit: PressureUnit
): Accuracy =
    if (this == ResolutionSettingsTarget.Pressure && pressureUnit == PressureUnit.Pascal) {
        Accuracy.Accuracy0
    } else {
        accuracyValues[this] ?: Accuracy.Accuracy2
    }

private fun ResolutionSettingsTarget.availableAccuracies(): List<Accuracy> =
    if (this == ResolutionSettingsTarget.ParticulateMatter) {
        listOf(Accuracy.Accuracy0, Accuracy.Accuracy1)
    } else {
        Accuracy.values().toList()
    }

private fun isResolutionTargetEnabled(
    target: ResolutionSettingsTarget,
    pressureUnit: PressureUnit
): Boolean =
    target != ResolutionSettingsTarget.Pressure || pressureUnit != PressureUnit.Pascal
