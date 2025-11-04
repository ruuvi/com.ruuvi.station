package com.ruuvi.station.network.ui.claim

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.components.dialog.CustomContentDialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.util.base.NfcActivity
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

@OptIn(ExperimentalAnimationApi::class)
class ClaimSensorActivity : NfcActivity(), KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: ClaimSensorViewModel by viewModel {
        intent.getStringExtra(SENSOR_ID)?.let {
            it
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            RuuviTheme {
                StatusBarFill{
                    Body()
                }
            }
        }

        viewModel.checkClaimState()
    }

    @Composable
    fun Body() {
        val context = LocalContext.current
        val navController = rememberAnimatedNavController()
        val titleString = stringResource(id = R.string.claim_sensor)
        val scaffoldState = rememberScaffoldState()
        val systemUiController = rememberSystemUiController()
        val systemBarsColor = RuuviStationTheme.colors.systemBars
        var title by remember { mutableStateOf(titleString) }

        LaunchedEffect(null) {
            viewModel.uiEvent.collect { uiEvent ->
                Timber.d("uiEvent $uiEvent")
                when (uiEvent) {
                    is UiEvent.Navigate -> {
                        if (uiEvent.popBackStack) {
                            navController.navigate(uiEvent.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                                launchSingleTop = true
                                restoreState = false
                            }
                        } else {
                            navController.navigate(uiEvent.route)
                        }
                    }
                    is UiEvent.ShowSnackbar -> {
                        scaffoldState.snackbarHostState.showSnackbar(uiEvent.message.asString(context))
                    }
                    is UiEvent.NavigateUp -> finish()
                    else -> {}
                }
            }
        }

        LaunchedEffect(navController) {
            navController.currentBackStackEntryFlow.collect { backStackEntry ->
                title = ClaimRoutes.getTitleByRoute(
                    context,
                    backStackEntry.destination.route ?: ""
                )
            }
        }

        Scaffold(
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxSize(),
            backgroundColor = RuuviStationTheme.colors.background,
            topBar = { RuuviTopAppBar(title = title) },
            scaffoldState = scaffoldState
        ) { padding ->

            AnimatedNavHost(
                navController = navController,
                startDestination = ClaimRoutes.CHECK_CLAIM_STATE
            ) {
                composable(
                    route = ClaimRoutes.NOT_SIGNED_IN,
                    enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(600)) },
                    exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(600)) },
                ) {
                    NotSignedInScreen()
                }

                composable(
                    route = ClaimRoutes.CHECK_CLAIM_STATE,
                    enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(600)) },
                    exitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(600)) },
                ) {
                    LoadingScreen(status = stringResource(id = R.string.check_claim_state))
                }

                composable(
                    route = ClaimRoutes.FREE_TO_CLAIM,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                ) {
                    ClaimSensor()
                }

                composable(
                    route = ClaimRoutes.UNCLAIM,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                ) {
                    UnclaimSensor()
                }

                composable(
                    route = ClaimRoutes.CLAIM_IN_PROGRESS,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                ) {
                    LoadingScreen(status = stringResource(id = R.string.claim_in_progress))
                }

                composable(
                    route = ClaimRoutes.FORCE_CLAIM_INIT,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                ) {
                    ForceClaimInit()
                }

                composable(
                    route = ClaimRoutes.FORCE_CLAIM_GETTING_ID,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                ) {
                    ForceClaimGettingId()
                }

                composable(
                    route = ClaimRoutes.FORCE_CLAIM_ERROR,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition
                ) {
                    ForceClaimError()
                }
            }
        }

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = systemBarsColor
            )
        }
    }

    @Composable
    fun NotSignedInScreen() {
        val context = LocalContext.current
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.claim_description))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.claim_ownership)) {
                        SignInActivity.start(context)
                    }
                }
            }
        }
    }

    @Composable
    fun ClaimSensor() {
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.claim_description))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.claim_ownership)) {
                        viewModel.claimSensor()
                    }
                }
            }
        }
    }

    @Composable
    fun UnclaimSensor() {
        var unclaimDialog by remember { mutableStateOf(false) }
        var deleteData by remember { mutableStateOf(false) }

        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.unclaim_sensor_description))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                RuuviCheckbox(
                    checked = deleteData,
                    text = stringResource(id = R.string.remove_cloud_history_description),
                    onCheckedChange = { checked -> deleteData = checked}
                )
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.unclaim)) {
                        unclaimDialog = true
                    }
                }
            }
        }

        if (unclaimDialog) {
            CustomContentDialog(
                title = stringResource(id = R.string.dialog_are_you_sure),
                onDismissRequest = { unclaimDialog = false },
                onOkClickAction = {
                    viewModel.unclaimSensor(deleteData)
                },
                positiveButtonText = stringResource(id = R.string.confirm)
            ) {
                Paragraph(text = stringResource(id = R.string.dialog_operation_undone))
            }
        }
    }

    @Composable
    fun ForceClaimInit() {
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.force_claim_sensor_description1))
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    RuuviButton(text = stringResource(id = R.string.force_claim)) {
                        viewModel.getSensorId()
                    }
                }
            }
        }
    }

    @Composable
    fun ForceClaimGettingId() {
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.force_claim_sensor_description2))
            }
        }

    }

    @Composable
    fun ForceClaimError() {
        PageSurfaceWithPadding() {
            Column() {
                Paragraph(text = stringResource(id = R.string.internet_connection_problem))
            }
        }

    }

    private val enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?) =
        { slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }

    private val exitTransition:  (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?) =
        { slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = tween(600)
        ) }

    companion object {
        private const val SENSOR_ID = "SENSOR_ID"

        fun start(context: Context, sensorId: String?) {
            val intent = Intent(context, ClaimSensorActivity::class.java)
            intent.putExtra(SENSOR_ID, sensorId)
            context.startActivity(intent)
        }
    }
}