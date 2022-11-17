package com.ruuvi.station.dashboard.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.about.ui.AboutActivity
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.app.ui.DashboardTopAppBar
import com.ruuvi.station.app.ui.MainMenu
import com.ruuvi.station.app.ui.MenuItem
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.network.ui.MyAccountActivity
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.settings.ui.SettingsActivity
import com.ruuvi.station.util.extensions.openUrl
import com.ruuvi.station.util.extensions.sendFeedback
import com.ruuvi.station.util.extensions.viewModel
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

class DashboardActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val dashboardViewModel: DashboardActivityViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme {
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val context = LocalContext.current
                val isDarkTheme = isSystemInDarkTheme()
                val scope = rememberCoroutineScope()
                val userEmail by dashboardViewModel.userEmail.observeAsState()
                val signedIn = !userEmail.isNullOrEmpty()

                Scaffold(
                    scaffoldState = scaffoldState,
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                    topBar = { DashboardTopAppBar(
                        actionCallBack = { AddTagActivity.start(context) },
                        navigationCallback = {
                            scope.launch {
                                scaffoldState.drawerState.open()
                            }
                        }
                    ) },
                    drawerContent = {
                                    MainMenu(
                                        items = listOf(
                                            MenuItem(R.string.menu_add_new_sensor, stringResource(id = R.string.menu_add_new_sensor)),
                                            MenuItem(R.string.menu_app_settings, stringResource(id = R.string.menu_app_settings)),
                                            MenuItem(R.string.menu_about_help, stringResource(id = R.string.menu_about_help)),
                                            MenuItem(R.string.menu_send_feedback, stringResource(id = R.string.menu_send_feedback)),
                                            MenuItem(R.string.menu_what_to_measure, stringResource(id = R.string.menu_what_to_measure)),
                                            MenuItem(R.string.menu_buy_sensors, stringResource(id = R.string.menu_buy_sensors)),
                                            MenuItem(R.string.menu_buy_gateway, stringResource(id = R.string.menu_buy_gateway)),
                                            if (signedIn) {
                                                MenuItem(R.string.my_ruuvi_account, stringResource(id = R.string.my_ruuvi_account))
                                            } else {
                                                MenuItem(R.string.sign_in, stringResource(id = R.string.sign_in))
                                            }
                                        ),
                                        onItemClick = { item ->
                                            when (item.id) {
                                                R.string.menu_add_new_sensor -> AddTagActivity.start(context)
                                                R.string.menu_app_settings -> SettingsActivity.start(context)
                                                R.string.menu_about_help -> AboutActivity.start(context)
                                                R.string.menu_send_feedback -> sendFeedback()
                                                R.string.menu_what_to_measure -> openUrl(getString(R.string.what_to_measure_link))
                                                R.string.menu_buy_sensors -> openUrl(getString(R.string.buy_sensors_link))
                                                R.string.menu_buy_gateway -> openUrl(getString(R.string.buy_gateway_link))
                                                R.string.my_ruuvi_account -> MyAccountActivity.start(context)
                                                R.string.sign_in -> SignInActivity.start(context)
                                            }
                                        }
                                    )
                                    },
                    drawerBackgroundColor = RuuviStationTheme.colors.background
                ) { paddingValues ->
                    Surface(
                        color = RuuviStationTheme.colors.dashboardBackground,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(RuuviStationTheme.dimensions.medium)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column() {
                            Card(
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                elevation = 0.dp,
                                backgroundColor = RuuviStationTheme.colors.dashboardCardBackground) {
                                Paragraph(text = "Sensor 1")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                elevation = 0.dp,
                                backgroundColor = RuuviStationTheme.colors.dashboardCardBackground) {
                                Paragraph(text = "Sensor 2")
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier
                                    .height(200.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                elevation = 0.dp,
                                backgroundColor = RuuviStationTheme.colors.dashboardCardBackground) {
                                Paragraph(text = "Sensor 3")
                            }
                        }

                    }
                }

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor,
                        darkIcons = !isDarkTheme
                    )
                }
            }
        }
    }

    companion object {
        fun start(context: Context) {
            val dashboardIntent = Intent(context, DashboardActivity::class.java)
            context.startActivity(dashboardIntent)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
}