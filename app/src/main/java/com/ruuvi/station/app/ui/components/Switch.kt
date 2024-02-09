package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun ruuviSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = RuuviStationTheme.colors.accent,
    uncheckedThumbColor = RuuviStationTheme.colors.inactive,
    checkedTrackColor = RuuviStationTheme.colors.trackColor,
    uncheckedTrackColor = RuuviStationTheme.colors.trackInactive,
    checkedTrackAlpha = 1f,
    uncheckedTrackAlpha = 1f
)

@Composable
fun SwitchRuuvi (
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier
        .fillMaxWidth()
        .clickable(role = Role.Switch) { onCheckedChange?.invoke(!checked) }
    ) {
        val (caption, switch) = createRefs()

        Text(
            text = text,
            textAlign = TextAlign.Left,
            style = RuuviStationTheme.typography.subtitle,
            modifier = Modifier.constrainAs(caption) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.absoluteLeft)
                end.linkTo(switch.start)
                width = Dimension.fillToConstraints
            }
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = ruuviSwitchColors(),
            modifier = Modifier.constrainAs(switch) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end)
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SwitchIndicatorRuuvi (
    text: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(modifier = modifier
        .fillMaxWidth()
        .clickable(role = Role.Switch) { onCheckedChange?.invoke(!checked) }
        .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
        .padding(
            vertical = RuuviStationTheme.dimensions.mediumPlus,
            horizontal = RuuviStationTheme.dimensions.screenPadding
        )
    ) {
        val (caption, onOff, switch) = createRefs()

        Text(
            text = text,
            textAlign = TextAlign.Left,
            style = RuuviStationTheme.typography.subtitle,
            modifier = Modifier.constrainAs(caption) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.absoluteLeft)
                end.linkTo(onOff.start)
                width = Dimension.fillToConstraints
            }
        )

        Text(
            text = if (checked) stringResource(id = R.string.on) else stringResource(id = R.string.off),
            textAlign = TextAlign.End,
            style = RuuviStationTheme.typography.paragraph,
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.medium)
                .constrainAs(onOff) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(caption.end)
                    end.linkTo(switch.start)
                    width = Dimension.fillToConstraints
                }

        )

        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = ruuviSwitchColors(),
                modifier = Modifier.constrainAs(switch) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                }
            )
        }
    }
}