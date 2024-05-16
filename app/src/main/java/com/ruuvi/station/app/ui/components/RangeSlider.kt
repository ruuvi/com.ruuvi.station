package com.ruuvi.station.app.ui.components

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.RangeSlider
import androidx.compose.material.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun RuuviSliderColors() = SliderDefaults.colors(
    thumbColor = RuuviStationTheme.colors.accent,
    activeTrackColor = RuuviStationTheme.colors.accent,
    inactiveTrackColor = RuuviStationTheme.colors.activeAlert
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RuuviRangeSlider(
    values: ClosedRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val start = if (valueRange.contains(values.start)) values.start else valueRange.start
    val end = if (valueRange.contains(values.endInclusive)) values.endInclusive else valueRange.endInclusive
    RangeSlider(
        value = minOf(start, end)..maxOf(start,end),
        onValueChange = {
            onValueChange.invoke(it.start..it.endInclusive)
        },
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = RuuviSliderColors()
    )
}