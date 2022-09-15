package com.ruuvi.station.app.ui.components

import androidx.annotation.FloatRange
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
    inactiveTrackColor = RuuviStationTheme.colors.divider
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
    RangeSlider(
        values = values.start..values.endInclusive,
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