package com.ruuvi.station.settings.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.navigate
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein

@OptIn(ExperimentalAnimationApi::class)
class SettingsActivity : AppCompatActivity(), KodeinAware {

    override val kodein by closestKodein()

    private val appSettingsListViewModel: AppSettingsListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme() {
                val navController = rememberAnimatedNavController()
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val activity = LocalContext.current as Activity
                var title: String by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        title = SettingsRoutes.getTitleByRoute(
                            activity,
                            backStackEntry.destination.route ?: ""
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.background,
                    topBar = { RuuviTopAppBar(title = title) },
                    scaffoldState = scaffoldState
                ) { padding ->
                    AnimatedNavHost(
                        navController = navController,
                        startDestination = SettingsRoutes.LIST
                    ) {
                        composable(SettingsRoutes.LIST,
                            enterTransition = { slideIntoContainer(towards = AnimatedContentScope.SlideDirection.Right, animationSpec = tween(600)) },
                            exitTransition = { slideOutOfContainer(towards = AnimatedContentScope.SlideDirection.Left, animationSpec = tween(600)) },
                        ) {
                            SettingsList(
                                scaffoldState = scaffoldState,
                                onNavigate = navController::navigate,
                                viewModel = appSettingsListViewModel
                            )
                        }
                        composable(
                            route = SettingsRoutes.APPEARANCE,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val appearanceSettingsViewModel: AppearanceSettingsViewModel by viewModel()
                            AppearanceSettings(
                                scaffoldState = scaffoldState,
                                viewModel = appearanceSettingsViewModel
                            )
                        }
                        composable(
                            route = SettingsRoutes.ALERT_NOTIFICATIONS,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            AlertNotificationsSettings(
                                scaffoldState = scaffoldState
                            )
                        }
                        composable(SettingsRoutes.BACKGROUNDSCAN,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val backgroundScanSettingsViewModel: BackgroundScanSettingsViewModel by viewModel()
                            BackgroundScanSettings(
                                scaffoldState = scaffoldState,
                                viewModel = backgroundScanSettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.TEMPERATURE,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val temperatureSettingsViewModel: TemperatureSettingsViewModel by viewModel()
                            TemperatureSettings(
                                scaffoldState = scaffoldState,
                                viewModel = temperatureSettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.HUMIDITY,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val humiditySettingsViewModel: HumiditySettingsViewModel by viewModel()
                            HumiditySettings(
                                scaffoldState = scaffoldState,
                                viewModel = humiditySettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.PRESSURE,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val pressureSettingsViewModel: PressureSettingsViewModel by viewModel()
                            PressureSettings(
                                scaffoldState = scaffoldState,
                                viewModel = pressureSettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.CLOUD,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val cloudSettingsViewModel: CloudSettingsViewModel by viewModel()
                            CloudSettings(
                                scaffoldState = scaffoldState,
                                viewModel = cloudSettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.CHARTS,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val chartSettingsViewModel: ChartSettingsViewModel by viewModel()
                            ChartSettings(
                                scaffoldState = scaffoldState,
                                viewModel = chartSettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.DATAFORWARDING,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val dataForwardingSettingsViewModel: DataForwardingSettingsViewModel by viewModel()
                            DataForwardingSettings(
                                scaffoldState = scaffoldState,
                                viewModel = dataForwardingSettingsViewModel
                            )
                        }
                        composable(SettingsRoutes.DEVELOPER,
                            enterTransition = enterTransition,
                            exitTransition = exitTransition
                        ) {
                            val developerSettingsViewModel: DeveloperSettingsViewModel by viewModel()
                            DeveloperSettings(
                                scaffoldState = scaffoldState,
                                viewModel = developerSettingsViewModel
                            )
                        }
                    }
                }

                val systemBarsColor = RuuviStationTheme.colors.systemBars
                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor,
                        darkIcons = false
                    )
                }
            }
        }
    }

    private val enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition?) =
        { slideIntoContainer(
            towards = AnimatedContentScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }
    private val exitTransition:  (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition?) =
        { slideOutOfContainer(
            towards = AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(600)
        ) }

    companion object {
        fun start(context: Context) {
            val settingsIntent = Intent(context, SettingsActivity::class.java)
            context.startActivity(settingsIntent)
        }
    }
}