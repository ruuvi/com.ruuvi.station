package com.ruuvi.station.network.ui.claim

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
        val navController = rememberNavController()
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

            NavHost(
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
        val bluetoothPermissions = remember { ForceClaimBluetooth.requiredPermissions() }

        val enableBluetoothLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (bluetoothAdapter(context)?.isEnabled == true) {
                viewModel.useBluetooth()
            } else {
                Timber.d("Bluetooth was not enabled, force claim read not started")
            }
        }

        fun startBluetoothWhenReady() {
            val adapter = bluetoothAdapter(context)
            when (
                ForceClaimBluetooth.nextStep(
                    permissionsGranted = bluetoothPermissions.all { isPermissionGranted(context, it) },
                    adapterAvailable = adapter != null,
                    adapterEnabled = adapter?.isEnabled == true,
                )
            ) {
                ForceClaimBluetooth.Step.REQUEST_PERMISSIONS -> permissionsInteractor.showPermissionSnackbar()
                ForceClaimBluetooth.Step.BLUETOOTH_UNAVAILABLE -> Timber.w("No Bluetooth adapter")
                ForceClaimBluetooth.Step.ENABLE_BLUETOOTH ->
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                ForceClaimBluetooth.Step.START_READ -> viewModel.useBluetooth()
            }
        }

        val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            startBluetoothWhenReady()
        }

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
                val hasNfc = remember { nfcAdapter(context) != null }
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
                            startNfcWhenReady(context)
                        }
                    }
                    RuuviButton(
                        modifier = if (hasNfc) Modifier.weight(1f) else Modifier.widthIn(min = 180.dp),
                        text = stringResource(id = R.string.use_bluetooth),
                        enabled = buttonsEnabled,
                        loading = bluetoothInProgress,
                    ) {
                        if (bluetoothPermissions.all { isPermissionGranted(context, it) }) {
                            startBluetoothWhenReady()
                        } else {
                            bluetoothPermissionLauncher.launch(bluetoothPermissions)
                        }
                    }
                }
            }
        }
    }

    private fun bluetoothAdapter(context: Context): BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private fun isPermissionGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private fun nfcAdapter(context: Context): NfcAdapter? = NfcAdapter.getDefaultAdapter(context)

    private fun startNfcWhenReady(context: Context) {
        val adapter = nfcAdapter(context)
        when (
            ForceClaimNfc.nextStep(
                adapterAvailable = adapter != null,
                adapterEnabled = adapter?.isEnabled == true,
            )
        ) {
            ForceClaimNfc.Step.NFC_UNAVAILABLE -> Timber.w("No NFC adapter")
            ForceClaimNfc.Step.ENABLE_NFC -> openNfcSettings()
            ForceClaimNfc.Step.START_WAIT -> viewModel.useNfc()
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