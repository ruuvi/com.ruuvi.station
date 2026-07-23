package com.ruuvi.station.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun AlertBadgeIcon(
    alarmStatus: AlarmSensorStatus,
    iconColor: Color,
    triggeredBadgeColor: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val triggered = alarmStatus is AlarmSensorStatus.Triggered
    val badgeCount = when (alarmStatus) {
        AlarmSensorStatus.NoAlarms -> 0
        is AlarmSensorStatus.NotTriggered -> alarmStatus.enabledCount
        is AlarmSensorStatus.Triggered -> alarmStatus.alarmTypes.size
    }

    Box(modifier = modifier.size(32.dp)) {
        Icon(
            modifier = Modifier
                .size(26.dp)
                .align(Alignment.CenterStart),
            painter = painterResource(id = R.drawable.ic_notifications_on_24px),
            tint = iconColor,
            contentDescription = contentDescription?.let { "$it: $badgeCount" },
        )

        if (badgeCount > 0) {
            if (triggered) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.TopEnd)
                        .background(
                            color = triggeredBadgeColor.copy(alpha = 1f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    AlertCount(
                        count = badgeCount,
                        color = Color.White,
                    )
                }
            } else {
                AlertCount(
                    count = badgeCount,
                    color = iconColor,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
    }
}

@Composable
private fun AlertCount(
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = count.toString(),
        color = color,
        fontSize = RuuviStationTheme.fontSizes.miniature,
        fontFamily = RuuviStationTheme.fonts.mulishExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}
