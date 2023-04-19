package com.ruuvi.station.app.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.PagerState
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.RadioButtonRuuvi
import com.ruuvi.station.app.ui.components.Subtitle
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.ruuviStationFontsSizes
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType

@Composable
fun RuuviTopAppBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val context = LocalContext.current as Activity

    TopAppBar(
        title = {
            Text(
                text = title,
                style = RuuviStationTheme.typography.topBarText,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                context.onBackPressed()
            }) {
                Icon(Icons.Default.ArrowBack, stringResource(id = R.string.back))
            }
        },
        actions = actions,
        backgroundColor = RuuviStationTheme.colors.topBar,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp
    )
}

@Composable
fun DashboardTopAppBar(
    dashboardType: DashboardType,
    dashboardTapAction: DashboardTapAction,
    navigationCallback: () -> Unit,
    changeDashboardType: (DashboardType) -> Unit,
    changeDashboardTapAction: (DashboardTapAction) -> Unit
) {
    var dashboardTypeMenuExpanded by remember {
        mutableStateOf(false)
    }

    TopAppBar(
        title = {
            Image(
                modifier = Modifier.height(40.dp),
                painter = painterResource(id = R.drawable.logo_2021),
                contentDescription = "",
                colorFilter = ColorFilter.tint(RuuviStationTheme.colors.dashboardIcons)
            )
        },
        navigationIcon = {
                         IconButton(
                             onClick = { navigationCallback.invoke() }) {
                             Icon(
                                 modifier = Modifier.size(36.dp),
                                 imageVector = ImageVector.vectorResource(id = R.drawable.ic_menu_24),
                                 tint = RuuviStationTheme.colors.dashboardBurger,
                                 contentDescription = ""
                             )
                         }
        },
        actions = {
            Box(
                modifier = Modifier
                    .clickable {
                        dashboardTypeMenuExpanded = true
                    }
                    .padding(end = RuuviStationTheme.dimensions.small)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        style = RuuviStationTheme.typography.subtitle,
                        text = stringResource(id = R.string.view)
                    )

                    Icon(
                        modifier = Modifier.padding(RuuviStationTheme.dimensions.medium),
                        painter = painterResource(id = R.drawable.drop_down_24),
                        contentDescription = "",
                        tint = RuuviStationTheme.colors.accent
                    )
                }

                DropdownMenu(
                    modifier = Modifier
                        .background(color = RuuviStationTheme.colors.background)
                        .padding(all = RuuviStationTheme.dimensions.medium),
                    expanded = dashboardTypeMenuExpanded,
                    onDismissRequest = { dashboardTypeMenuExpanded = false }) {

                    Subtitle(
                        modifier = Modifier.padding(
                            horizontal = RuuviStationTheme.dimensions.mediumPlus,
                            vertical = RuuviStationTheme.dimensions.medium
                        ),
                        text = stringResource(id = R.string.card_type)
                    )

                    RadioButtonRuuvi(
                        text = stringResource(id = R.string.image_cards),
                        isSelected = dashboardType == DashboardType.IMAGE_VIEW,
                        onClick = {
                            changeDashboardType.invoke(DashboardType.IMAGE_VIEW)
                        }
                    )

                    RadioButtonRuuvi(
                        text = stringResource(id = R.string.simple_cards),
                        isSelected = dashboardType == DashboardType.SIMPLE_VIEW,
                        onClick = {
                            changeDashboardType.invoke(DashboardType.SIMPLE_VIEW)
                        }
                    )

                    Subtitle(
                        modifier = Modifier.padding(
                            horizontal = RuuviStationTheme.dimensions.mediumPlus,
                            vertical = RuuviStationTheme.dimensions.medium
                        ),
                        text = stringResource(id = R.string.card_action)
                    )

                    RadioButtonRuuvi(
                        text = stringResource(id = R.string.open_sensor_view),
                        isSelected = dashboardTapAction == DashboardTapAction.OPEN_CARD,
                        onClick = {
                            changeDashboardTapAction.invoke(DashboardTapAction.OPEN_CARD)
                        }
                    )

                    RadioButtonRuuvi(
                        text = stringResource(id = R.string.open_history_view),
                        isSelected = dashboardTapAction == DashboardTapAction.SHOW_CHART,
                        onClick = {
                            changeDashboardTapAction.invoke(DashboardTapAction.SHOW_CHART)
                        }
                    )
                }
            }
        },
        backgroundColor = RuuviStationTheme.colors.dashboardBackground,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp
    )
}

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingTopAppBar(
    pagerState: PagerState,
    actionCallBack: () -> Unit
) {
    TopAppBar(
        title = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()
            ) {

                HorizontalPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier
                        .padding(16.dp)
                )

                Text(
                    modifier = Modifier
                        .padding(horizontal = RuuviStationTheme.dimensions.mediumPlus)
                        .align(Alignment.CenterEnd)
                        .clickable { actionCallBack.invoke() },
                    style = RuuviStationTheme.typography.topBarText,
                    text = "Skip",
                    fontSize = ruuviStationFontsSizes.normal,
                    textDecoration = TextDecoration.Underline
                )
            }
        },
        elevation = 0.dp,
        backgroundColor = Color.Transparent,
        contentColor = RuuviStationTheme.colors.topBarText,
        )
}