package com.ruuvi.station.app.permissions

import android.Manifest.permission.*
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.*
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.components.dialog.RuuviPermissionDialog
import com.ruuvi.station.util.extensions.locationEnabled
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun BluetoothPermissions(
    scaffoldState: ScaffoldState,
    askToEnableBluetooth: Boolean,
    askForBackgroundLocation: Boolean,
    preferencesRepository: PreferencesRepository
) {
    Timber.d("BluetoothPermissions current API = ${Build.VERSION.SDK_INT} askToEnableBluetooth = $askToEnableBluetooth askForBackgroundLocation = $askForBackgroundLocation")
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        NearbyDevicesPermissions(scaffoldState, askToEnableBluetooth, preferencesRepository)
    } else {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            LocationBluetoothPermissionsAndroid11(scaffoldState, askToEnableBluetooth, askForBackgroundLocation, preferencesRepository)
        } else {
            LocationBluetoothPermissions(scaffoldState, askToEnableBluetooth, preferencesRepository)
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationBluetoothPermissions(
    scaffoldState: ScaffoldState,
    askToEnableBluetooth: Boolean,
    preferencesRepository: PreferencesRepository
) {
    Timber.d("LocationBluetoothPermissions askToEnableBluetooth = $askToEnableBluetooth")
    val context = LocalContext.current

    var showBluetoothPermissionDialog by remember {
        mutableStateOf(false)
    }

    val rememberLocationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Timber.d("EnableBluetooth result $it")
    }

    val enableLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Timber.d("EnableLocation result $it")
    }

    val bluetoothConnectPermissionState = rememberMultiplePermissionsState(
        listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
    ) {
        Timber.d("Permission request result:")
        for (item in it) {
            Timber.d("${item.key} - ${item.value}")
        }

        if (it.all { item -> item.value }) {
            Timber.d("LocationBluetoothPermissions are granted!")
            if (askToEnableBluetooth) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                if (!rememberLocationManager.locationEnabled()) {
                    enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
        } else {
            Timber.d("LocationBluetoothPermissions are NOT granted")
            permissionSnackbar(context, scaffoldState, context.getString(R.string.permission_location_needed))
        }
    }


    if (showBluetoothPermissionDialog) {
        RuuviPermissionDialog(
            title = stringResource(id = R.string.permission_dialog_title),
            message = stringResource(id = R.string.permission_dialog_request_message),
            onAccept = {
                bluetoothConnectPermissionState.launchMultiplePermissionRequest()
            },
            onDismissRequest = {
                showBluetoothPermissionDialog = false
            }
        )
    }

    LaunchedEffect(key1 = null) {
        Timber.d("Checking BT permissions")
        if (bluetoothConnectPermissionState.allPermissionsGranted) {
            Timber.d("LocationBluetoothPermissions are already granted")
            if (askToEnableBluetooth) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                if (!rememberLocationManager.locationEnabled()) {
                    enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
        } else {
            Timber.d("LocationBluetoothPermissions requesting")
            if (bluetoothConnectPermissionState.shouldShowRationale || !preferencesRepository.isBluetoothPermissionRequested()) {
                showBluetoothPermissionDialog = true
                preferencesRepository.bluetoothPermissionRequested()
            } else {
                bluetoothConnectPermissionState.launchMultiplePermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationBluetoothPermissionsAndroid11(
    scaffoldState: ScaffoldState,
    askToEnableBluetooth: Boolean,
    askForBackgroundLocation: Boolean,
    preferencesRepository: PreferencesRepository
) {
    Timber.d("LocationBluetoothPermissions askToEnableBluetooth = $askToEnableBluetooth askForBackgroundLocation = $askForBackgroundLocation")
    val context = LocalContext.current

    var showBluetoothPermissionDialog by remember {
        mutableStateOf(false)
    }

    var showBackgroundBluetoothPermissionDialog by remember {
        mutableStateOf(false)
    }

    val rememberLocationManager = remember {
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Timber.d("EnableBluetooth result $it")
    }

    val enableLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Timber.d("EnableLocation result $it")
    }

    val backgroundBluetoothPermissionState = rememberPermissionState(permission = ACCESS_BACKGROUND_LOCATION) { result ->
        Timber.d("Background permission request result: $result")
        if (!result) {
            permissionSnackbar(context, scaffoldState, context.getString(R.string.permission_location_needed))
        }

    }

    val bluetoothConnectPermissionState = rememberMultiplePermissionsState(
        listOf(ACCESS_COARSE_LOCATION, ACCESS_FINE_LOCATION)
    ) {
        Timber.d("Permission request result:")
        for (item in it) {
            Timber.d("${item.key} - ${item.value}")
        }

        if (it.all { item -> item.value }) {
            Timber.d("LocationBluetoothPermissions are granted!")
            if (askToEnableBluetooth) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                if (!rememberLocationManager.locationEnabled()) {
                    enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
            if (askForBackgroundLocation &&
                Build.VERSION.SDK_INT == Build.VERSION_CODES.R &&
                !backgroundBluetoothPermissionState.status.isGranted) {

                Timber.d("BackgroundLocation shouldShowRationale = ${backgroundBluetoothPermissionState.status.shouldShowRationale}")
                if (!backgroundBluetoothPermissionState.status.isGranted) {
                    showBackgroundBluetoothPermissionDialog = true
                }
            }
        } else {
            Timber.d("LocationBluetoothPermissions are NOT granted")
            permissionSnackbar(context, scaffoldState, context.getString(R.string.permission_location_needed))
        }
    }


    if (showBluetoothPermissionDialog) {
        RuuviPermissionDialog (
            title = stringResource(id = R.string.permission_dialog_title),
            message = stringResource(id = R.string.permission_dialog_request_message),
            onAccept = {
                bluetoothConnectPermissionState.launchMultiplePermissionRequest() },
            onDismissRequest = {
                showBluetoothPermissionDialog = false
            }
        )
    }

    if (showBackgroundBluetoothPermissionDialog) {
        RuuviPermissionDialog(
            title = stringResource(id = R.string.permission_background_dialog_title),
            message = stringResource(id = R.string.permission_dialog_background_request_message),
            onAccept = {
                backgroundBluetoothPermissionState.launchPermissionRequest( ) },
            onDismissRequest = {
                showBackgroundBluetoothPermissionDialog = false
            }
        )
    }

    LaunchedEffect(key1 = null) {
        Timber.d("Checking BT permissions")
        if (bluetoothConnectPermissionState.allPermissionsGranted) {
            Timber.d("LocationBluetoothPermissions are already granted")
            if (askToEnableBluetooth) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                if (!rememberLocationManager.locationEnabled()) {
                    enableLocationLauncher.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            }
            if (askForBackgroundLocation &&
                Build.VERSION.SDK_INT == Build.VERSION_CODES.R &&
                !backgroundBluetoothPermissionState.status.isGranted) {

                Timber.d("BackgroundLocation isGranted = ${backgroundBluetoothPermissionState.status.isGranted} shouldShowRationale = ${backgroundBluetoothPermissionState.status.shouldShowRationale}")
                if (!backgroundBluetoothPermissionState.status.isGranted) {
                    showBackgroundBluetoothPermissionDialog = true
                }
            }
        } else {
            Timber.d("LocationBluetoothPermissions requesting")
            if (bluetoothConnectPermissionState.shouldShowRationale || !preferencesRepository.isBluetoothPermissionRequested()) {
                showBluetoothPermissionDialog = true
                preferencesRepository.bluetoothPermissionRequested()
            } else {
                bluetoothConnectPermissionState.launchMultiplePermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NearbyDevicesPermissions(
    scaffoldState: ScaffoldState,
    askToEnableBluetooth: Boolean,
    preferencesRepository: PreferencesRepository
) {
    Timber.d("NearbyDevicesPermissions askToEnableBluetooth = $askToEnableBluetooth")
    val context = LocalContext.current

    var showBluetoothPermissionDialog by remember {
        mutableStateOf(false)
    }

    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Timber.d("EnableBluetooth result $it")
    }

    val bluetoothConnectPermissionState = rememberMultiplePermissionsState(
        listOf(BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
    ) {
        if (it.all { item -> item.value }) {
            Timber.d("NearbyDevicesPermissions are granted!")
            if (askToEnableBluetooth) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } else {
            Timber.d("NearbyDevicesPermissions are NOT granted")
            permissionSnackbar(context, scaffoldState, context.getString(R.string.permission_location_needed_api31))
        }
    }

    if (showBluetoothPermissionDialog) {
        RuuviPermissionDialog(
            title = stringResource(id = R.string.permission_dialog_title),
            message = stringResource(id = R.string.permission_dialog_request_message_api31),
            onAccept = {
                bluetoothConnectPermissionState.launchMultiplePermissionRequest()
            },
            onDismissRequest = {
                showBluetoothPermissionDialog = false
            }
        )
    }

    LaunchedEffect(key1 = null) {
        Timber.d("Checking BT permissions")
        if (bluetoothConnectPermissionState.allPermissionsGranted) {
            Timber.d("NearbyDevicesPermissions are already granted")
            if (askToEnableBluetooth) {
                enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            }
        } else {
            Timber.d("NearbyDevicesPermissions requesting")
            if (bluetoothConnectPermissionState.shouldShowRationale || !preferencesRepository.isBluetoothPermissionRequested()) {
                showBluetoothPermissionDialog = true
                preferencesRepository.bluetoothPermissionRequested()
            } else {
                bluetoothConnectPermissionState.launchMultiplePermissionRequest()
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermission(
    scaffoldState: ScaffoldState,
    shouldAskNotificationPermission: Boolean,
    nextCheckReady: () -> Unit
) {
    Timber.d("NotificationPermission API = ${Build.VERSION.SDK_INT}")
    val context = LocalContext.current

    val notificationPermissionState = rememberPermissionState(POST_NOTIFICATIONS) { result ->
        if (result) {
            Timber.d("NotificationPermission are granted!")
        } else {
            Timber.d("NotificationPermission are NOT granted")
            permissionSnackbar(context, scaffoldState, context.getString(R.string.notification_permission_needed))
        }
        nextCheckReady.invoke()
    }

    LaunchedEffect(key1 = null) {
        Timber.d("Checking notification permissions")
        if (shouldAskNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !notificationPermissionState.status.isGranted
        ) {
            notificationPermissionState.launchPermissionRequest()
        } else {
            nextCheckReady.invoke()
        }
    }

}


fun permissionSnackbar(context: Context, scaffoldState: ScaffoldState, message: String) {
    CoroutineScope(Dispatchers.Main).launch {
        val actionResult = scaffoldState.snackbarHostState.showSnackbar(
            message = message,
            actionLabel = context.getString(R.string.settings),
            duration = SnackbarDuration.Long
        )
        if (actionResult == SnackbarResult.ActionPerformed) {
            val intent = Intent()
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = uri
            context.startActivity(intent)
        }
    }
}

fun isBluetoothEnabled(context: Context): Boolean {
    val bluetoothManager: BluetoothManager = context.getSystemService(BluetoothManager::class.java)
    return bluetoothManager.adapter?.isEnabled ?: false
}