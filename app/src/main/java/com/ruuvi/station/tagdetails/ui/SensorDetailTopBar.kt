package com.ruuvi.station.tagdetails.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.app.ui.components.AlertBadgeIcon
import com.ruuvi.station.app.ui.components.CircularIndicator
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.tag.domain.RuuviTag
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private val SensorMenuButtonWidth = 40.dp
private val SensorMenuButtonSpacing = 2.dp
private val SensorMenuIndicatorWidth = 16.dp
private val SensorDetailHeaderHeight = 44.dp
private val SensorTitleTopPadding = 10.dp
private val SensorTitleBottomPadding = 6.dp
private val SensorTitleTouchTargetExpansion = 8.dp
private val SensorTitleArrowTopOffset = 2.dp
private val SensorTitleArrowTouchSize = 48.dp
private val SensorTitleArrowSize = 16.dp
private val SensorTitleLineHeight = 24.sp

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(SensorDetailHeaderHeight)
            .background(
                if (useOpaqueBackground) RuuviStationTheme.colors.topBar else Color.Transparent,
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.size(SensorDetailHeaderHeight),
                onClick = onBack,
            ) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(id = R.drawable.arrow_back_16),
                    tint = iconColor,
                    contentDescription = stringResource(id = R.string.back),
                )
            }

            Image(
                modifier = Modifier.size(width = 90.dp, height = 22.dp),
                painter = painterResource(id = R.drawable.logo_2021),
                contentDescription = null,
                colorFilter = ColorFilter.tint(iconColor),
            )
        }

        SensorDetailMenu(
            destination = destination,
            alarmStatus = alarmStatus,
            iconColor = iconColor,
            onDestinationSelected = onDestinationSelected,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
        )

        if (syncInProgress) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SensorDetailHeaderHeight),
                contentAlignment = Alignment.Center,
            ) {
                CircularIndicator(color = Color.White.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun SensorDetailMenu(
    destination: SensorDetailDestination,
    alarmStatus: AlarmSensorStatus,
    iconColor: Color,
    onDestinationSelected: (SensorDetailDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val indicatorOffset by animateDpAsState(
        targetValue = (SensorMenuButtonWidth + SensorMenuButtonSpacing) * destination.ordinal +
            (SensorMenuButtonWidth - SensorMenuIndicatorWidth) / 2,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "sensor detail menu indicator",
    )

    Box(
        modifier = modifier
            .width(
                SensorMenuButtonWidth * SensorDetailDestination.entries.size +
                    SensorMenuButtonSpacing * (SensorDetailDestination.entries.size - 1),
            )
            .height(SensorDetailHeaderHeight),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(SensorMenuButtonSpacing),
        ) {
            DestinationAction(
                selected = destination == SensorDetailDestination.CARD,
                onClick = { onDestinationSelected(SensorDetailDestination.CARD) },
            ) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_sensor_menu_measurement),
                    tint = iconColor,
                    contentDescription = stringResource(id = R.string.full_image_view),
                )
            }
            DestinationAction(
                selected = destination == SensorDetailDestination.HISTORY,
                onClick = { onDestinationSelected(SensorDetailDestination.HISTORY) },
            ) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_sensor_menu_graph),
                    tint = iconColor,
                    contentDescription = stringResource(id = R.string.history_view),
                )
            }
            DestinationAction(
                selected = destination == SensorDetailDestination.ALERTS,
                onClick = { onDestinationSelected(SensorDetailDestination.ALERTS) },
            ) {
                AlertBadgeIcon(
                    alarmStatus = alarmStatus,
                    iconColor = iconColor,
                    triggeredBadgeColor = RuuviStationTheme.colors.activeAlertThemed,
                    contentDescription = stringResource(id = R.string.alerts),
                    iconRes = R.drawable.ic_sensor_menu_alerts,
                    iconSize = 30.dp,
                )
            }
            DestinationAction(
                selected = destination == SensorDetailDestination.SETTINGS,
                onClick = { onDestinationSelected(SensorDetailDestination.SETTINGS) },
            ) {
                Icon(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_sensor_menu_settings),
                    tint = iconColor,
                    contentDescription = stringResource(id = R.string.sensor_settings),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .padding(bottom = 4.dp)
                .width(SensorMenuIndicatorWidth)
                .height(2.dp)
                .background(
                    color = RuuviStationTheme.colors.topBarText,
                    shape = RoundedCornerShape(1.dp),
                ),
        )
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
            .width(SensorMenuButtonWidth)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier
                .size(SensorMenuButtonWidth)
                .semantics {
                    this.selected = selected
                },
            onClick = onClick,
        ) {
            content()
        }
    }
}

@Composable
internal fun SensorDetailTitle(
    sensors: List<RuuviTag>,
    pagePosition: Float,
    subtitle: String?,
    canSelectPrevious: Boolean,
    canSelectNext: Boolean,
    onSelectPrevious: () -> Unit,
    onSelectNext: () -> Unit,
    modifier: Modifier = Modifier,
    swipeModifier: Modifier = Modifier,
    contentColor: Color = Color.White,
) {
    val labelHorizontalPadding =
        RuuviStationTheme.dimensions.huge - SensorTitleArrowTouchSize

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = SensorTitleTopPadding,
                    bottom = SensorTitleBottomPadding + SensorTitleTouchTargetExpansion,
                )
                .padding(horizontal = SensorTitleArrowTouchSize)
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SensorTitlePager(
                sensors = sensors,
                pagePosition = pagePosition,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(swipeModifier),
                contentHorizontalPadding = labelHorizontalPadding,
                contentColor = contentColor,
            )
            AnimatedContent(
                modifier = Modifier.padding(horizontal = labelHorizontalPadding),
                targetState = subtitle,
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 200)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 150))
                },
                contentAlignment = Alignment.TopCenter,
                label = "sensor detail subtitle",
            ) { visibleSubtitle ->
                visibleSubtitle?.let {
                    Text(
                        modifier = Modifier.padding(top = 2.dp),
                        text = it,
                        fontSize = RuuviStationTheme.fontSizes.small,
                        fontFamily = RuuviStationTheme.fonts.mulishBold,
                        textAlign = TextAlign.Center,
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }

        Box(modifier = Modifier.matchParentSize()) {
            if (canSelectPrevious) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .size(SensorTitleArrowTouchSize),
                    onClick = onSelectPrevious,
                ) {
                    Icon(
                        modifier = Modifier
                            .offset(y = -SensorTitleArrowTopOffset)
                            .size(SensorTitleArrowSize),
                        painter = painterResource(id = R.drawable.arrow_back_16),
                        contentDescription = null,
                        tint = contentColor,
                    )
                }
            }

            if (canSelectNext) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(SensorTitleArrowTouchSize),
                    onClick = onSelectNext,
                ) {
                    Icon(
                        modifier = Modifier
                            .offset(y = -SensorTitleArrowTopOffset)
                            .size(SensorTitleArrowSize),
                        painter = painterResource(id = R.drawable.arrow_forward_16),
                        contentDescription = null,
                        tint = contentColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorTitlePager(
    sensors: List<RuuviTag>,
    pagePosition: Float,
    modifier: Modifier = Modifier,
    contentHorizontalPadding: Dp,
    contentColor: Color,
) {
    if (sensors.isEmpty()) return

    val constrainedPosition = pagePosition.coerceIn(0f, (sensors.size - 1).toFloat())
    val firstPage = floor(constrainedPosition).toInt()
    val lastPage = ceil(constrainedPosition).toInt()
    val visiblePages = if (firstPage == lastPage) {
        listOf(firstPage)
    } else {
        listOf(firstPage, lastPage)
    }
    val selectedName = sensors[constrainedPosition.roundToInt()].displayName

    Layout(
        modifier = modifier
            .clipToBounds()
            .semantics {
                contentDescription = selectedName
            },
        content = {
            visiblePages.forEach { page ->
                key(sensors[page].id) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = contentHorizontalPadding)
                            .clearAndSetSemantics { },
                        text = sensors[page].displayName,
                        fontSize = RuuviStationTheme.fontSizes.big,
                        lineHeight = SensorTitleLineHeight,
                        fontFamily = RuuviStationTheme.fonts.mulishExtraBold,
                        textAlign = TextAlign.Center,
                        color = contentColor,
                        maxLines = 2,
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(childConstraints) }
        val pageFraction = constrainedPosition - firstPage
        val firstHeight = placeables.first().height
        val lastHeight = placeables.last().height
        val interpolatedHeight = (
            firstHeight + (lastHeight - firstHeight) * pageFraction
        ).roundToInt()
        val layoutHeight = interpolatedHeight.coerceIn(
            constraints.minHeight,
            constraints.maxHeight,
        )

        layout(constraints.maxWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val page = visiblePages[index]
                val x = ((page - constrainedPosition) * constraints.maxWidth).roundToInt()
                placeable.placeRelative(x = x, y = 0)
            }
        }
    }
}
