package com.ruuvi.station.settings.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.util.extensions.enableRuuviEdgeToEdge
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI

class SettingsActivity : AppCompatActivity(), DIAware {

    override val di by closestDI()

    private val appSettingsListViewModel: AppSettingsListViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableRuuviEdgeToEdge()

        setContent {
            RuuviTheme() {
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()
                val context = LocalContext.current
                var title: String by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        title = SettingsRoutes.getTitleByRoute(
                            context,
                            backStackEntry.destination.route ?: ""
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(RuuviStationTheme.colors.topBar)
                ) {
                    Scaffold(
                        modifier = Modifier
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                            )
                            .fillMaxSize(),
                        backgroundColor = RuuviStationTheme.colors.background,
                        topBar = { RuuviTopAppBar(title = title) },
                        scaffoldState = scaffoldState,
                        snackbarHost = {
                            SnackbarHost(
                                hostState = it,
                                modifier = Modifier.navigationBarsPadding(),
                            )
                        },
                    ) { padding ->
                        NavHost(
                            modifier = Modifier.padding(padding),
                            navController = navController,
                            startDestination = SettingsRoutes.LIST
                        ) {
                            composable(
                                SettingsRoutes.LIST,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                SettingsList(
                                    scaffoldState = scaffoldState,
                                    onNavigate = navController::navigate,
                                    viewModel = appSettingsListViewModel
                                )
                            }
                            composable(
                                route = SettingsRoutes.APPEARANCE,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val appearanceSettingsViewModel: AppearanceSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    AppearanceSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = appearanceSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                route = SettingsRoutes.ALERT_NOTIFICATIONS,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val alertNotificationsSettingsViewModel: AlertNotificationsSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    AlertNotificationsSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = alertNotificationsSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.BACKGROUNDSCAN,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val backgroundScanSettingsViewModel: BackgroundScanSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    BackgroundScanSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = backgroundScanSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.TEMPERATURE,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val temperatureSettingsViewModel: TemperatureSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    TemperatureSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = temperatureSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.HUMIDITY,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val humiditySettingsViewModel: HumiditySettingsViewModel by viewModel()
                                SettingsSubPage {
                                    HumiditySettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = humiditySettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.PRESSURE,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val pressureSettingsViewModel: PressureSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    PressureSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = pressureSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.GLOBAL_UNITS,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val globalUnitsAndResolutionViewModel: GlobalUnitsAndResolutionViewModel by viewModel()
                                SettingsSubPage {
                                    GlobalUnitsSettings(
                                        scaffoldState = scaffoldState,
                                        onNavigate = navController::navigate,
                                        viewModel = globalUnitsAndResolutionViewModel
                                    )
                                }
                            }
                            composable(
                                route = SettingsRoutes.GLOBAL_UNIT_SELECT,
                                arguments = listOf(
                                    navArgument(SettingsRoutes.GLOBAL_UNIT_TYPE_ARG) {
                                        type = NavType.StringType
                                    }
                                ),
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) { backStackEntry ->
                                val globalUnitsAndResolutionViewModel: GlobalUnitsAndResolutionViewModel by viewModel()
                                SettingsSubPage {
                                    GlobalUnitSelectionSettings(
                                        scaffoldState = scaffoldState,
                                        unitType = backStackEntry.arguments?.getString(
                                            SettingsRoutes.GLOBAL_UNIT_TYPE_ARG
                                        ),
                                        viewModel = globalUnitsAndResolutionViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.RESOLUTION,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val globalUnitsAndResolutionViewModel: GlobalUnitsAndResolutionViewModel by viewModel()
                                SettingsSubPage {
                                    ResolutionSettings(
                                        scaffoldState = scaffoldState,
                                        onNavigate = navController::navigate,
                                        viewModel = globalUnitsAndResolutionViewModel
                                    )
                                }
                            }
                            composable(
                                route = SettingsRoutes.RESOLUTION_SELECT,
                                arguments = listOf(
                                    navArgument(SettingsRoutes.RESOLUTION_TARGET_ARG) {
                                        type = NavType.StringType
                                    }
                                ),
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) { backStackEntry ->
                                val globalUnitsAndResolutionViewModel: GlobalUnitsAndResolutionViewModel by viewModel()
                                SettingsSubPage {
                                    ResolutionSelectionSettings(
                                        scaffoldState = scaffoldState,
                                        target = backStackEntry.arguments?.getString(
                                            SettingsRoutes.RESOLUTION_TARGET_ARG
                                        ),
                                        viewModel = globalUnitsAndResolutionViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.CLOUD,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val cloudSettingsViewModel: CloudSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    CloudSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = cloudSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.CHARTS,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val chartSettingsViewModel: ChartSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    ChartSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = chartSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.DATAFORWARDING,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val dataForwardingSettingsViewModel: DataForwardingSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    DataForwardingSettings(
                                        scaffoldState = scaffoldState,
                                        viewModel = dataForwardingSettingsViewModel
                                    )
                                }
                            }
                            composable(
                                SettingsRoutes.DEVELOPER,
                                enterTransition = pushEnterTransition,
                                exitTransition = pushExitTransition,
                                popEnterTransition = popEnterTransition,
                                popExitTransition = popExitTransition
                            ) {
                                val developerSettingsViewModel: DeveloperSettingsViewModel by viewModel()
                                SettingsSubPage {
                                    DeveloperSettings(
                                        scaffoldState = scaffoldState,
                                        onNavigate = navController::navigate,
                                        viewModel = developerSettingsViewModel
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsSubPage(content: @Composable () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            content()
        }
    }

    private val pushEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) =
        { slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }
    private val pushExitTransition:  (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) =
        { slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }
    private val popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) =
        { slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(600)
        ) }
    private val popExitTransition:  (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) =
        { slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(600)
        ) }

    companion object {
        fun start(context: Context) {
            val settingsIntent = Intent(context, SettingsActivity::class.java)
            context.startActivity(settingsIntent)
        }
    }
}
