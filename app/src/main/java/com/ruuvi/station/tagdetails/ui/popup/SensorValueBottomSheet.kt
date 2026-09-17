package com.ruuvi.station.tagdetails.ui.popup

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.model.EnvironmentValue
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.vico.model.ChartData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Both sensor-card layouts use the same inputs, history and explanation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorValueBottomSheet(
    sensor: RuuviTag,
    value: EnvironmentValue,
    maxHeight: Int,
    getChartData: (String, UnitType, Int) -> Flow<ChartData>,
    scrollToChart: (UnitType) -> Unit,
    onChangeValue: (EnvironmentValue) -> Unit,
    onDismiss: () -> Unit
) {
    val chartHistory by produceState<ChartData?>(null, sensor.id, value.unitType) {
        getChartData(sensor.id, value.unitType, 48).collectLatest { this.value = it }
    }
    val inputs = when (value.unitType) {
        is UnitType.AirQuality -> listOfNotNull(sensor.latestMeasurement?.pm25, sensor.latestMeasurement?.co2)
        is UnitType.MouldRisk -> listOfNotNull(sensor.latestMeasurement?.temperature, sensor.latestMeasurement?.humidity)
        else -> emptyList()
    }
    ValueBottomSheet(
        sheetValue = if (value.unitType is UnitType.MouldRisk) {
            sensor.valuesToDisplay.firstOrNull { it.unitType == value.unitType } ?: value
        } else value,
        extraValues = inputs,
        chartHistory = chartHistory,
        maxHeight = maxHeight,
        lastUpdate = sensor.latestMeasurement?.updatedAt,
        scrollToChart = scrollToChart,
        onChangeValue = onChangeValue,
        onDismiss = onDismiss
    )
}
