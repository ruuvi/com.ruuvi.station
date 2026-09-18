package com.ruuvi.station.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    @DrawableRes iconRes: Int = R.drawable.ic_notifications_on_24px,
    iconSize: Dp = 26.dp,
) {
    val triggered = alarmStatus is AlarmSensorStatus.Triggered
    val badgeCount = when (alarmStatus) {
        AlarmSensorStatus.NoAlarms -> 0
        is AlarmSensorStatus.NotTriggered -> alarmStatus.enabledCount
        is AlarmSensorStatus.Triggered -> alarmStatus.alarmTypes.size
    }
    val badgeText = if (badgeCount > 99) "99+" else badgeCount.toString()

    // Matches the shared iOS alert-bell geometry: a 30 dp icon in a 36 dp
    // container, with the badge anchored four points inside the icon's edge.
    Box(modifier = modifier.size(36.dp)) {
        Icon(
            modifier = Modifier
                .size(iconSize)
                .align(Alignment.Center),
            painter = painterResource(id = iconRes),
            tint = iconColor,
            contentDescription = contentDescription?.let {
                if (badgeCount > 0) "$it: $badgeText" else it
            },
        )

        if (badgeCount > 0) {
            val badgeModifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 11.dp, y = 7.dp)

            if (triggered) {
                Box(
                    modifier = badgeModifier
                        .height(15.dp)
                        .widthIn(min = 15.dp)
                        .background(
                            color = triggeredBadgeColor.copy(alpha = 1f),
                            shape = RoundedCornerShape(percent = 50),
                        )
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AlertCount(
                        text = badgeText,
                        color = Color.White,
                    )
                }
            } else {
                AlertCount(
                    text = badgeText,
                    color = iconColor,
                    modifier = badgeModifier,
                )
            }
        }
    }
}

@Composable
private fun AlertCount(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        color = color,
        fontSize = 10.sp,
        lineHeight = 10.sp,
        fontFamily = RuuviStationTheme.fonts.mulishExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}
