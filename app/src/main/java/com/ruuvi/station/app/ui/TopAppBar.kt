package com.ruuvi.station.app.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
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
    navigationCallback: () -> Unit,
    actionCallBack: (DashboardType) -> Unit
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
                modifier = Modifier.clickable {
                    dashboardTypeMenuExpanded = true
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        style = RuuviStationTheme.typography.subtitle,
                        text = stringResource(id = R.string.view)
                    )

                    Icon(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(id = R.drawable.drop_down_24),
                        contentDescription = "",
                        tint = RuuviStationTheme.colors.accent
                    )
                }

                DropdownMenu(
                    modifier = Modifier.background(color = RuuviStationTheme.colors.background),
                    expanded = dashboardTypeMenuExpanded,
                    onDismissRequest = { dashboardTypeMenuExpanded = false }) {

                    DropdownMenuItem(onClick = {
                        actionCallBack.invoke(DashboardType.IMAGE_VIEW)
                        dashboardTypeMenuExpanded = false
                    }) {
                        Paragraph(text = stringResource(id = R.string.image_view))
                    }

                    DropdownMenuItem(onClick = {
                        actionCallBack.invoke(DashboardType.SIMPLE_VIEW)
                        dashboardTypeMenuExpanded = false
                    }) {
                        Paragraph(text = stringResource(id = R.string.simple_view))
                    }
                }
            }
        },
        backgroundColor = RuuviStationTheme.colors.dashboardBackground,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp
    )
}