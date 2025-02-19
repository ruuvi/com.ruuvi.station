package com.ruuvi.station.alarm.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmState
import com.ruuvi.station.app.ui.components.BlinkingEffect

@Composable
fun AlarmStateIndicator(
    modifier: Modifier = Modifier,
    alarmState: AlarmState,
    baseIconSize: Dp = 10.dp
) {
    val iconSize = baseIconSize * LocalDensity.current.fontScale.coerceAtMost(1.5f)

    Box (
        modifier = modifier
            .size(iconSize),
        contentAlignment = Center
    ) {
        when (alarmState) {
            AlarmState.TRIGGERED -> {
                BlinkingEffect() {
                    Image(
                        painter = painterResource(id = R.drawable.alert_bell_triggered),
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize)
                    )
                }
            }

            AlarmState.SET -> {
                Image(
                    painter = painterResource(id = R.drawable.alert_bell),
                    contentDescription = null,
                    modifier = Modifier
                        .size(iconSize)
                )
            }

            AlarmState.NO_ALARM -> {}
        }
    }
}