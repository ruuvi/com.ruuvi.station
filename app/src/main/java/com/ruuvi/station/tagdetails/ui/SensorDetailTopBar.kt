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

private val SENSOR_MENU_BUTTON_WIDTH = 40.dp
private val SENSOR_MENU_BUTTON_SPACING = 2.dp
private val SENSOR_MENU_INDICATOR_WIDTH = 16.dp
private val SENSOR_DETAIL_HEADER_HEIGHT = 44.dp
private val SENSOR_TITLE_TOP_PADDING = 10.dp
private val SENSOR_TITLE_BOTTOM_PADDING = 6.dp
private val SENSOR_TITLE_TOUCH_TARGET_EXPANSION = 8.dp
private val SENSOR_TITLE_ARROW_TOP_OFFSET = 2.dp
private val SENSOR_TITLE_ARROW_TOUCH_SIZE = 48.dp
private val SENSOR_TITLE_ARROW_SIZE = 16.dp
private val SENSOR_TITLE_LINE_HEIGHT = 24.sp
private val SENSOR_MENU_ICON_SIZE = 30.dp
private const val SENSOR_MENU_INDICATOR_DAMPING_RATIO = 0.8f
private const val SENSOR_TITLE_FADE_IN_MILLIS = 200
private const val SENSOR_TITLE_FADE_OUT_MILLIS = 150

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
            .height(SENSOR_DETAIL_HEADER_HEIGHT)
            .background(
                if (useOpaqueBackground) RuuviStationTheme.colors.topBar else Color.Transparent,
            ),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                modifier = Modifier.size(SENSOR_DETAIL_HEADER_HEIGHT),
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
                    .height(SENSOR_DETAIL_HEADER_HEIGHT),
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
    val destinations = SensorDetailDestination.entries
    val indicatorOffset by animateDpAsState(
        targetValue = (SENSOR_MENU_BUTTON_WIDTH + SENSOR_MENU_BUTTON_SPACING) *
            destinations.indexOf(destination) +
            (SENSOR_MENU_BUTTON_WIDTH - SENSOR_MENU_INDICATOR_WIDTH) / 2,
        animationSpec = spring(
            dampingRatio = SENSOR_MENU_INDICATOR_DAMPING_RATIO,
            stiffness = Spring.StiffnessLow,
        ),
        label = "sensor detail menu indicator",
    )

    Box(
        modifier = modifier
            .width(
                SENSOR_MENU_BUTTON_WIDTH * destinations.size +
                    SENSOR_MENU_BUTTON_SPACING * (destinations.size - 1),
            )
            .height(SENSOR_DETAIL_HEADER_HEIGHT),
    ) {
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.spacedBy(SENSOR_MENU_BUTTON_SPACING),
        ) {
            destinations.forEach { menuDestination ->
                DestinationAction(
                    selected = destination == menuDestination,
                    onClick = { onDestinationSelected(menuDestination) },
                ) {
                    when (menuDestination) {
                        SensorDetailDestination.CARD -> Icon(
                            modifier = Modifier.size(SENSOR_MENU_ICON_SIZE),
                            painter = painterResource(id = R.drawable.ic_sensor_menu_measurement),
                            tint = iconColor,
                            contentDescription = stringResource(id = R.string.full_image_view),
                        )
                        SensorDetailDestination.HISTORY -> Icon(
                            modifier = Modifier.size(SENSOR_MENU_ICON_SIZE),
                            painter = painterResource(id = R.drawable.ic_sensor_menu_graph),
                            tint = iconColor,
                            contentDescription = stringResource(id = R.string.history_view),
                        )
                        SensorDetailDestination.ALERTS -> AlertBadgeIcon(
                            alarmStatus = alarmStatus,
                            iconColor = iconColor,
                            triggeredBadgeColor = RuuviStationTheme.colors.activeAlertThemed,
                            contentDescription = stringResource(id = R.string.alerts),
                            iconRes = R.drawable.ic_sensor_menu_alerts,
                            iconSize = SENSOR_MENU_ICON_SIZE,
                        )
                        SensorDetailDestination.SETTINGS -> Icon(
                            modifier = Modifier.size(SENSOR_MENU_ICON_SIZE),
                            painter = painterResource(id = R.drawable.ic_sensor_menu_settings),
                            tint = iconColor,
                            contentDescription = stringResource(id = R.string.sensor_settings),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .padding(bottom = 4.dp)
                .width(SENSOR_MENU_INDICATOR_WIDTH)
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
            .width(SENSOR_MENU_BUTTON_WIDTH)
            .fillMaxHeight(),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(
            modifier = Modifier
                .size(SENSOR_MENU_BUTTON_WIDTH)
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
        RuuviStationTheme.dimensions.huge - SENSOR_TITLE_ARROW_TOUCH_SIZE

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = SENSOR_TITLE_TOP_PADDING,
                    bottom = SENSOR_TITLE_BOTTOM_PADDING + SENSOR_TITLE_TOUCH_TARGET_EXPANSION,
                )
                .padding(horizontal = SENSOR_TITLE_ARROW_TOUCH_SIZE)
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
                    fadeIn(animationSpec = tween(durationMillis = SENSOR_TITLE_FADE_IN_MILLIS)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = SENSOR_TITLE_FADE_OUT_MILLIS))
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
                        .size(SENSOR_TITLE_ARROW_TOUCH_SIZE),
                    onClick = onSelectPrevious,
                ) {
                    Icon(
                        modifier = Modifier
                            .offset(y = -SENSOR_TITLE_ARROW_TOP_OFFSET)
                            .size(SENSOR_TITLE_ARROW_SIZE),
                        painter = painterResource(id = R.drawable.arrow_back_16),
                        contentDescription = stringResource(id = R.string.previous_sensor),
                        tint = contentColor,
                    )
                }
            }

            if (canSelectNext) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(SENSOR_TITLE_ARROW_TOUCH_SIZE),
                    onClick = onSelectNext,
                ) {
                    Icon(
                        modifier = Modifier
                            .offset(y = -SENSOR_TITLE_ARROW_TOP_OFFSET)
                            .size(SENSOR_TITLE_ARROW_SIZE),
                        painter = painterResource(id = R.drawable.arrow_forward_16),
                        contentDescription = stringResource(id = R.string.next_sensor),
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
                        lineHeight = SENSOR_TITLE_LINE_HEIGHT,
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
