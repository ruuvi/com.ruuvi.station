package com.ruuvi.station.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.network.ui.MyAccountActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.SettingsActivity
import com.ruuvi.station.util.extensions.openUrl
import com.ruuvi.station.util.extensions.sendFeedback
import kotlinx.coroutines.launch

@Composable
fun MainMenu(
    items: List<MenuItem>,
    onItemClick: (MenuItem) -> Unit
) {
    LazyColumn() {
        itemsIndexed(items) { index, item ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemClick.invoke(item)
                    }
                    .padding(
                        top = RuuviStationTheme.dimensions.mediumPlus,
                        bottom = RuuviStationTheme.dimensions.mediumPlus,
                        start = RuuviStationTheme.dimensions.extended,
                        end = RuuviStationTheme.dimensions.extended
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = item.title, style = RuuviStationTheme.typography.menuItem)
            }
            if (index < items.lastIndex) {
                DividerRuuvi()
            }
        }
    }
}

data class MenuItem(
    val id: Int,
    val title: String
)


@Composable
fun DashboardMainMenu(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState,
    signedIn: Boolean,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    MainMenu(
        items = listOf(
            MenuItem(
                R.string.menu_add_new_sensor,
                stringResource(id = R.string.menu_add_new_sensor)
            ),
            MenuItem(
                R.string.menu_app_settings,
                stringResource(id = R.string.menu_app_settings)
            ),
            MenuItem(
                R.string.menu_about_help,
                stringResource(id = R.string.menu_about_help)
            ),
            MenuItem(
                R.string.menu_send_feedback,
                stringResource(id = R.string.menu_send_feedback)
            ),
            MenuItem(
                R.string.menu_what_to_measure,
                stringResource(id = R.string.menu_what_to_measure)
            ),
            MenuItem(
                R.string.menu_buy_sensors,
                stringResource(id = R.string.menu_buy_sensors)
            ),
            if (signedIn) {
                MenuItem(
                    R.string.my_ruuvi_account,
                    stringResource(id = R.string.my_ruuvi_account)
                )
            } else {
                MenuItem(
                    R.string.sign_in,
                    stringResource(id = R.string.sign_in)
                )
            }
        ),
        onItemClick = { item ->
            when (item.id) {
                R.string.menu_add_new_sensor -> AddTagActivity.start(context)
                R.string.menu_app_settings -> SettingsActivity.start(context)
                R.string.menu_about_help -> AboutActivity.start(context)
                R.string.menu_send_feedback -> context.sendFeedback()
                R.string.menu_what_to_measure -> context.openUrl(context.getString(R.string.what_to_measure_link))
                R.string.menu_buy_sensors -> context.openUrl(context.getString(R.string.buy_sensors_menu_link))
                R.string.my_ruuvi_account -> MyAccountActivity.start(context)
                R.string.sign_in -> SignInActivity.start(context)
            }
            scope.launch {
                scaffoldState.drawerState.close()
            }
        }
    )
}