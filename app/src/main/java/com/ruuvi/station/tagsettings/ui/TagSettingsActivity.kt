package com.ruuvi.station.tagsettings.ui

import android.app.Activity
import android.content.*
import android.os.*
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.TaskStackBuilder
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.components.StatusBarFill
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.tagsettings.di.RemoveSensorViewModelArgs
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurements
import com.ruuvi.station.tagsettings.ui.visible_measurements.VisibleMeasurementsViewModel
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class TagSettingsActivity : AppCompatActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: TagSettingsViewModel by viewModel {
        intent.getStringExtra(TAG_ID)?.let {
            TagSettingsViewModelArgs(
                tagId = it,
                newSensor = intent.getBooleanExtra(NEW_SENSOR, false),
                openRemove = intent.getBooleanExtra(OPEN_REMOVE, false)
            )
        }
    }

    private val alarmsViewModel: AlarmItemsViewModel by viewModel {
        intent.getStringExtra(TAG_ID)
    }

    private var timer: Timer? = null

    private val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) =
        { slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }
    private val exitTransition:  (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) =
        { slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = tween(600)
        ) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val openRemove = intent.getBooleanExtra(OPEN_REMOVE, false)
        setContent {
            RuuviTheme {
                val context = LocalContext.current
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.systemBars
                val navController = rememberNavController()
                var title: String by rememberSaveable { mutableStateOf("") }
                val activity = LocalContext.current as Activity

                LaunchedEffect(navController) {
                    navController.currentBackStackEntryFlow.collect { backStackEntry ->
                        title = SensorSettingsRoutes.getTitleByRoute(
                            activity,
                            backStackEntry.destination.route ?: ""
                        )
                    }
                }

                StatusBarFill {
                    Scaffold(
                        modifier = Modifier
                            .systemBarsPadding()
                            .fillMaxSize(),
                        backgroundColor = RuuviStationTheme.colors.background,
                        topBar = {
                            RuuviTopAppBar(
                                title = title
                            )
                        },
                        scaffoldState = scaffoldState
                    ) { paddingValues ->

                        NavHost(
                            modifier = Modifier.padding(paddingValues),
                            startDestination = if (openRemove) {
                                SensorSettingsRoutes.SENSOR_REMOVE
                            } else {
                                SensorSettingsRoutes.SENSOR_SETTINGS_ROOT
                            },
                            navController = navController
                        ) {
                            composable(
                                SensorSettingsRoutes.SENSOR_SETTINGS_ROOT,
                                enterTransition = {
                                    slideIntoContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                                        animationSpec = tween(600)
                                    )
                                },
                                exitTransition = {
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                                        animationSpec = tween(600)
                                    )
                                },
                            ) {
                                SensorSettings(
                                    scaffoldState = scaffoldState,
                                    viewModel = viewModel,
                                    onNavigate = navController::navigate,
                                    alarmsViewModel = alarmsViewModel
                                )
                            }
                            composable(
                                route = SensorSettingsRoutes.SENSOR_REMOVE,
                                enterTransition = enterTransition,
                                exitTransition = exitTransition
                            ) {
                                val removeSensorViewModel: RemoveSensorViewModel by viewModel() {
                                    intent.getStringExtra(TAG_ID)?.let {
                                        RemoveSensorViewModelArgs(it)
                                    }
                                }
                                RemoveSensor(
                                    scaffoldState = scaffoldState,
                                    viewModel = removeSensorViewModel,
                                    onNavigate = navController::navigate
                                )
                            }

                            composable(
                                route = SensorSettingsRoutes.VISIBLE_MEASUREMENTS,
                                enterTransition = enterTransition,
                                exitTransition = exitTransition
                            ) {
                                val visibleMeasurementsViewModel: VisibleMeasurementsViewModel by viewModel() {
                                    intent.getStringExtra(TAG_ID)?.let {
                                        it
                                    }
                                }
                                val useDefault by visibleMeasurementsViewModel.useDefaultOrder.collectAsStateWithLifecycle()
                                val sensorState by visibleMeasurementsViewModel.sensorState.collectAsStateWithLifecycle()
                                val selected by visibleMeasurementsViewModel.selected.collectAsStateWithLifecycle()
                                val possibleOptions by visibleMeasurementsViewModel.possibleOptions.collectAsStateWithLifecycle()

                                VisibleMeasurements(
                                    useDefault = useDefault,
                                    sensorState = sensorState,
                                    dashboardType = visibleMeasurementsViewModel.dashBoardType,
                                    onAction = visibleMeasurementsViewModel::onAction,
                                    effects = visibleMeasurementsViewModel.effects,
                                    getUnitName = visibleMeasurementsViewModel::getUnitName,
                                    selected = selected,
                                    allOptions = possibleOptions
                                )
                            }
                        }


                    }
                }

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        timer = Timer("TagSettingsActivityTimer", true)
        timer?.scheduleAtFixedRate(0, 1000) {
            viewModel.getTagInfo()
        }
        viewModel.checkIfSensorShared()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
    }

    companion object {
        private const val TAG_ID = "TAG_ID"
        private const val SCROLL_TO_ALARMS = "SCROLL_TO_ALARMS"
        private const val NEW_SENSOR = "NEW_SENSOR"
        private const val OPEN_REMOVE = "OPEN_REMOVE"

        fun start(context: Context, tagId: String?, scrollToAlarms: Boolean = false) {
            val intent = Intent(context, TagSettingsActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            intent.putExtra(SCROLL_TO_ALARMS, scrollToAlarms)
            context.startActivity(intent)
        }

        fun startAfterAddingNewSensor(context: Context, tagId: String?) {
            val dashboardIntent = Intent(context, DashboardActivity::class.java)

            val sensorCardIntent = Intent(context, SensorCardActivity::class.java)
            sensorCardIntent.putExtra(SensorCardActivity.ARGUMENT_SENSOR_ID, tagId)

            val settingsIntent = Intent(context, TagSettingsActivity::class.java)
            settingsIntent.putExtra(TAG_ID, tagId)
            settingsIntent.putExtra(NEW_SENSOR, true)

            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addNextIntent(dashboardIntent)
            stackBuilder.addNextIntent(sensorCardIntent)
            stackBuilder.addNextIntent(settingsIntent)
            stackBuilder.startActivities()
        }

        fun startToRemove(context: Context, tagId: String?) {
            val intent = Intent(context, TagSettingsActivity::class.java)
            intent.putExtra(TAG_ID, tagId)
            intent.putExtra(OPEN_REMOVE, true)
            context.startActivity(intent)
        }
    }
}