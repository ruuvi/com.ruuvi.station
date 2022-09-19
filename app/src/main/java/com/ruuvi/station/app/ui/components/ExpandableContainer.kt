package com.ruuvi.station.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.RectangleShape
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@ExperimentalMaterialApi
@Composable
fun ExpandableTextTitleContainer(
    title: String,
    content: @Composable () -> Unit
) {
    ExpandableContainer(
        header = {
            Subtitle(
                    text = title,
                )
        },
        content = content
    )
}

@ExperimentalMaterialApi
@Composable
fun ExpandableContainer(
    header: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    var expandedState by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expandedState) 180f else 0f
    )

    Card (
        backgroundColor = RuuviStationTheme.colors.background,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearOutSlowInEasing
                )
            ),
        shape = RectangleShape,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .background(color = RuuviStationTheme.colors.settingsSubTitle)
                    .height(RuuviStationTheme.dimensions.sensorSettingTitleHeight)
                    .padding(RuuviStationTheme.dimensions.medium)
                    .clickable { expandedState = !expandedState },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    header()
                }

                IconButton(
                    modifier = Modifier
                        .rotate(rotationState),
                    onClick = {
                        expandedState = !expandedState
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "",
                        tint = RuuviStationTheme.colors.accent
                    )
                }
            }
            if (expandedState) {
                content()
            }
        }

    }
}
