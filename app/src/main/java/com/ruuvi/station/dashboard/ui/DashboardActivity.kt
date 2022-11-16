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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.addtag.ui.AddTagActivity
import com.ruuvi.station.app.ui.DashboardTopAppBar
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.settings.ui.SettingsActivity

class DashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme() {
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.dashboardBackground
                val context = LocalContext.current
                val isDarkTheme = isSystemInDarkTheme()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.dashboardBackground,
                    topBar = { DashboardTopAppBar(
                        actionCallBack = { AddTagActivity.start(context) },
                        navigationCallback = { SettingsActivity.start(context) }
                    ) },
                    scaffoldState = scaffoldState
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