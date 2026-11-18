package com.ruuvi.station.network.ui.claim

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ruuvi.station.R
import com.ruuvi.station.app.permissions.PermissionsInteractor
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.components.dialog.CustomContentDialog
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.network.ui.SignInActivity
import com.ruuvi.station.util.ui.RuuviSnackbarHost
import com.ruuvi.station.util.base.NfcActivity
import com.ruuvi.station.util.extensions.enableRuuviEdgeToEdge
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.android.closestDI
import timber.log.Timber

@OptIn(ExperimentalAnimationApi::class)
class ClaimSensorActivity : NfcActivity(), DIAware {

    override val di: DI by closestDI()

    private val viewModel: ClaimSensorViewModel by viewModel {
        intent.getStringExtra(SENSOR_ID) ?: ""
    }

    private lateinit var permissionsInteractor: PermissionsInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableRuuviEdgeToEdge()
        permissionsInteractor = PermissionsInteractor(this)

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
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .fillMaxSize(),
            backgroundColor = RuuviStationTheme.colors.background,
            topBar = { RuuviTopAppBar(title = title) },
            scaffoldState = scaffoldState,
            snackbarHost = { RuuviSnackbarHost(hostState = it, modifier = Modifier.navigationBarsPadding()) },
        ) { padding ->

            AnimatedNavHost(
                modifier = Modifier.padding(padding).navigationBarsPadding(),
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
        val context = LocalContext.current
        val bluetoothInProgress by viewModel.bluetoothInProgress.collectAsState()
        val nfcInProgress by viewModel.nfcInProgress.collectAsState()
        val buttonsEnabled = !bluetoothInProgress && !nfcInProgress

        Surface(
            color = RuuviStationTheme.colors.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    val instruction = if (viewModel.isAir == true) {
                        stringResource(R.string.force_claim_air_description2)
                    } else {
                        stringResource(id = R.string.force_claim_sensor_description2)
                    }
                    Paragraph(text = instruction)
                }
                Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))
                // Like iOS: without NFC hardware only the Bluetooth option is offered.
                val hasNfc = remember { NfcAdapter.getDefaultAdapter(context) != null }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (hasNfc) {
                        Arrangement.spacedBy(RuuviStationTheme.dimensions.extended)
                    } else {
                        Arrangement.Center
                    }
                ) {
                    if (hasNfc) {
                        RuuviButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(id = R.string.use_nfc),
                            enabled = buttonsEnabled,
                            loading = nfcInProgress,
                        ) {
                            if (NfcAdapter.getDefaultAdapter(context)?.isEnabled != true) {
                                openNfcSettings()
                            } else {
                                viewModel.useNfc()
                            }
                        }
                    }
                    RuuviButton(
                        modifier = if (hasNfc) Modifier.weight(1f) else Modifier.widthIn(min = 180.dp),
                        text = stringResource(id = R.string.use_bluetooth),
                        enabled = buttonsEnabled,
                        loading = bluetoothInProgress,
                    ) {
                        permissionsInteractor.requestPermissions(needBackground = false, askForBluetooth = true)
                        viewModel.useBluetooth()
                    }
                }
            }
        }
    }

    private fun openNfcSettings() {
        try {
            startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
            startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
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