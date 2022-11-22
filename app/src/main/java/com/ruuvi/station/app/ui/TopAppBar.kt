package com.ruuvi.station.app.ui

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
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
import com.ruuvi.station.app.ui.theme.RuuviStationTheme

@Composable
fun MyTopAppBar(
    title: String
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
        backgroundColor = RuuviStationTheme.colors.topBar,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp
    )
}

@Composable
fun DashboardTopAppBar(
    navigationCallback: () -> Unit,
    actionCallBack: () -> Unit
) {
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
            IconButton(onClick = { actionCallBack.invoke() }) {
                Icon(
                    modifier = Modifier.size(36.dp),
                    imageVector = ImageVector.vectorResource(id = R.drawable.add),
                    tint = RuuviStationTheme.colors.dashboardIcons,
                    contentDescription = ""
                )
            }
        },
        backgroundColor = RuuviStationTheme.colors.dashboardBackground,
        contentColor = RuuviStationTheme.colors.topBarText,
        elevation = 0.dp
    )
}