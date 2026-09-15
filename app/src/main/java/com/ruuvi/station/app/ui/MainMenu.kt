package com.ruuvi.station.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.app.ui.components.DividerRuuvi
import com.ruuvi.station.app.ui.components.RuuviButton
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
    onItemClick: (MenuItem) -> Unit,
    footer: (@Composable () -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
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

        footer?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                it()
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
    showNewsletter: Boolean,
) {
    val context = LocalContext.current
    val newsletterUrl = stringResource(id = R.string.newsletter_url)
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
                R.string.my_ruuvi_account -> MyAccountActivity.start(context)
                R.string.sign_in -> SignInActivity.start(context)
            }
            scope.launch {
                scaffoldState.drawerState.close()
            }
        },
        footer = if (showNewsletter) {
            {
                NewsletterCard {
                    if (signedIn) {
                        MyAccountActivity.start(context)
                    } else {
                        context.openUrl(newsletterUrl)
                    }
                    scope.launch {
                        scaffoldState.drawerState.close()
                    }
                }
            }
        } else null
    )
}

@Composable
private fun NewsletterCard(onSubscribe: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(
                start = RuuviStationTheme.dimensions.extended,
                end = RuuviStationTheme.dimensions.extended,
                bottom = RuuviStationTheme.dimensions.extended
            )
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(top = 52.dp)
                .fillMaxWidth()
                .background(
                    color = RuuviStationTheme.colors.settingsSubTitle,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(
                    start = RuuviStationTheme.dimensions.extended,
                    top = 68.dp,
                    end = RuuviStationTheme.dimensions.extended,
                    bottom = RuuviStationTheme.dimensions.extended
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.newsletter_card_title),
                style = RuuviStationTheme.typography.subtitle,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.medium))
            Text(
                text = stringResource(id = R.string.newsletter_card_description),
                style = RuuviStationTheme.typography.paragraphSmall,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.mediumPlus))
            RuuviButton(
                modifier = Modifier.fillMaxWidth(0.75f),
                text = stringResource(id = R.string.newsletter_subscribe),
                height = RuuviStationTheme.dimensions.buttonHeightSmall,
                onClick = onSubscribe
            )
        }

        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.signin_beaver_mail),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(118.dp)
                .height(132.dp)
        )
    }
}
