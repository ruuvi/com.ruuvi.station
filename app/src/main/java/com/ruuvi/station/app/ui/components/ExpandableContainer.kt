package com.ruuvi.station.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun ExpandableTextTitleContainer(
    title: String,
    backgroundColor: Color = RuuviStationTheme.colors.background,
    content: @Composable () -> Unit
) {
    ExpandableContainer(
        header = {
            Subtitle(
                    text = title,
                )
        },
        backgroundColor = backgroundColor,
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@ExperimentalMaterialApi
@Composable
fun ExpandableContainer(
    header: @Composable () -> Unit,
    backgroundColor: Color = RuuviStationTheme.colors.settingsSubTitle,
    content: @Composable () -> Unit
) {
    var expandedState by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (expandedState) 180f else 0f
    )
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = RuuviStationTheme.colors.background)
            .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
            .animateContentSize(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = LinearOutSlowInEasing
                )
            ),
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expandedState = !expandedState }
                .background(color = backgroundColor)
                .defaultMinSize(minHeight = RuuviStationTheme.dimensions.sensorSettingTitleHeight)
                .padding(
                    horizontal = RuuviStationTheme.dimensions.screenPadding,
                    vertical = RuuviStationTheme.dimensions.mediumPlus
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                header()
            }

            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
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
        }
        if (expandedState) {
            Column(modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)) {
                content()
            }
            coroutineScope.launch {
                delay(50)
                bringIntoViewRequester.bringIntoView()
            }
        }
    }
}
