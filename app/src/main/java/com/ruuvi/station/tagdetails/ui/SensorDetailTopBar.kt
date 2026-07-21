package com.ruuvi.station.tagdetails.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.ui.components.BlinkingEffect
import com.ruuvi.station.app.ui.components.CircularIndicator
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.tag.domain.RuuviTag

@Composable
internal fun SensorDetailTopAppBar(
    destination: SensorDetailDestination,
    syncInProgress: Boolean,
    alarmStatus: AlarmSensorStatus,
    useOpaqueBackground: Boolean,
    onBack: () -> Unit,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
) {
    val iconColor = RuuviStationTheme.colors.topBarText

    Box {
        TopAppBar(
            modifier = Modifier.height(RuuviStationTheme.dimensions.topAppBarHeight),
            title = {
                Image(
                    modifier = Modifier.height(40.dp),
                    painter = painterResource(id = R.drawable.logo_2021),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(iconColor),
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                    )
                }
            },
            actions = {
                DestinationAction(
                    selected = destination == SensorDetailDestination.CARD,
                    onClick = { onDestinationSelected(SensorDetailDestination.CARD) },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_menu_temperature),
                        tint = iconColor,
                        contentDescription = stringResource(id = R.string.full_image_view),
                    )
                }
                DestinationAction(
                    selected = destination == SensorDetailDestination.HISTORY,
                    onClick = { onDestinationSelected(SensorDetailDestination.HISTORY) },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_ruuvi_graphs_icon),
                        tint = iconColor,
                        contentDescription = stringResource(id = R.string.history_view),
                    )
                }
                DestinationAction(
                    selected = destination == SensorDetailDestination.ALERTS,
                    onClick = { onDestinationSelected(SensorDetailDestination.ALERTS) },
                ) {
                    AlertIcon(
                        alarmStatus = alarmStatus,
                        defaultColor = iconColor,
                    )
                }
                DestinationAction(
                    selected = destination == SensorDetailDestination.SETTINGS,
                    onClick = { onDestinationSelected(SensorDetailDestination.SETTINGS) },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_settings_24px),
                        tint = iconColor,
                        contentDescription = stringResource(id = R.string.sensor_settings),
                    )
                }
            },
            backgroundColor = if (useOpaqueBackground) {
                RuuviStationTheme.colors.topBar
            } else {
                Color.Transparent
            },
            contentColor = RuuviStationTheme.colors.topBarText,
            elevation = 0.dp,
        )

        if (syncInProgress) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(RuuviStationTheme.dimensions.topAppBarHeight),
                contentAlignment = Alignment.Center,
            ) {
                CircularIndicator(color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun AlertIcon(
    alarmStatus: AlarmSensorStatus,
    defaultColor: Color,
) {
    val contentDescription = stringResource(id = R.string.alerts)
    when (alarmStatus) {
        AlarmSensorStatus.NoAlarms -> Icon(
            painter = painterResource(id = R.drawable.ic_notifications_off_24px),
            tint = defaultColor,
            contentDescription = contentDescription,
        )
        AlarmSensorStatus.NotTriggered -> Icon(
            painter = painterResource(id = R.drawable.ic_notifications_on_24px),
            tint = defaultColor,
            contentDescription = contentDescription,
        )
        is AlarmSensorStatus.Triggered -> BlinkingEffect {
            Icon(
                painter = painterResource(id = R.drawable.ic_notifications_active_24px),
                tint = RuuviStationTheme.colors.activeAlert,
                contentDescription = contentDescription,
            )
        }
    }
}

@Composable
private fun DestinationAction(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(48.dp)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            enabled = !selected,
            onClick = onClick,
        ) {
            content()
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
                    .width(24.dp)
                    .height(3.dp)
                    .background(
                        color = RuuviStationTheme.colors.topBarText,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        }
    }
}

@Composable
internal fun SensorDetailTitle(
    sensor: RuuviTag,
    subtitle: String?,
    canSelectPrevious: Boolean,
    canSelectNext: Boolean,
    onSelectPrevious: () -> Unit,
    onSelectNext: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = if (subtitle == null) 8.dp else 16.dp),
    ) {
        if (canSelectPrevious) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
                    .size(RuuviStationTheme.dimensions.buttonHeightSmall),
                onClick = onSelectPrevious,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_back_16),
                    contentDescription = null,
                    tint = contentColor,
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = RuuviStationTheme.dimensions.huge)
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = sensor.displayName,
                fontSize = RuuviStationTheme.fontSizes.big,
                fontFamily = RuuviStationTheme.fonts.mulishExtraBold,
                textAlign = TextAlign.Center,
                color = contentColor,
                maxLines = 2,
            )
            subtitle?.let {
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = it,
                    fontSize = RuuviStationTheme.fontSizes.extended,
                    fontFamily = RuuviStationTheme.fonts.mulishRegular,
                    textAlign = TextAlign.Center,
                    color = contentColor,
                    maxLines = 1,
                )
            }
        }

        if (canSelectNext) {
            IconButton(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
                    .size(RuuviStationTheme.dimensions.buttonHeightSmall),
                onClick = onSelectNext,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow_forward_16),
                    contentDescription = null,
                    tint = contentColor,
                )
            }
        }
    }
}
