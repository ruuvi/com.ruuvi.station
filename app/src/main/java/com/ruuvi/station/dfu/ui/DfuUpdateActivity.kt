package com.ruuvi.station.dfu.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.*
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.permissions.PermissionsInteractor
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.UiEvent
import timber.log.Timber

class DfuUpdateActivity : AppCompatActivity() , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: DfuUpdateViewModel by viewModel { TagSettingsViewModelArgs(intent.getStringExtra(SENSOR_ID) ?:"") }

    private val viewModelAir: DfuAirUpdateViewModel by viewModel { intent.getStringExtra(SENSOR_ID) }

    private lateinit var permissionsInteractor: PermissionsInteractor

    val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedFileUri: Uri? = result.data?.data
            if (selectedFileUri != null) {
                // Handle the selected file URI
                Timber.d("File selected $selectedFileUri")
                val fileName = getFileName(this, selectedFileUri)
                Timber.d("File selected filename $fileName")
                //viewModel.upload(fileName!!, readBytesFromUri(this, selectedFileUri)!!)
            }
        }
    }
    fun selectFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/octet-stream" // Allow all file types
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent) // Step 3: Launch the file picker
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        if (fileName == null) {
            fileName = uri.path?.substringAfterLast('/')
        }
        return fileName
    }

    fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val sensorId = intent.getStringExtra(SENSOR_ID)
        permissionsInteractor = PermissionsInteractor(this)

        setContent {
            val deviceType by viewModel.deviceType.collectAsState()

            RuuviTheme {
                val navController = rememberNavController()
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()

                val systemBarsColor = RuuviStationTheme.colors.systemBars

                SideEffect {
                    systemUiController.setSystemBarsColor(
                        color = systemBarsColor,
                        darkIcons = false
                    )
                }

                StatusBarFill {
                    if (deviceType == DeviceType.AIR) {
                        LaunchedEffect(null) {
                            viewModelAir.uiEvent.collect { uiEvent ->
                                Timber.d("uiEvent $uiEvent")
                                when (uiEvent) {
                                    is UiEvent.NavigateNew -> {
                                        if (uiEvent.popBackStack) {
                                            navController.navigate(uiEvent.route) {
                                                popUpToRoute
                                            }
                                        } else {
                                            navController.navigate(uiEvent.route)
                                        }
                                    }
                                    is UiEvent.ShowSnackbar -> {
                                        scaffoldState.snackbarHostState.showSnackbar(
                                            message = uiEvent.message.asString(this@DfuUpdateActivity),
                                            actionLabel = getString(R.string.ok)
                                        )
                                    }
                                    is UiEvent.NavigateUp -> navController.navigateUp()
                                    else -> {}
                                }
                            }
                        }

                        Scaffold(
                            modifier = Modifier
                                .systemBarsPadding()
                                .fillMaxSize(),
                            backgroundColor = RuuviStationTheme.colors.background,
                            topBar = { RuuviTopAppBar(title = stringResource(R.string.title_activity_dfu_update)) },
                            scaffoldState = scaffoldState
                        ) { padding ->
                            NavHost(
                                modifier = Modifier.padding(padding),
                                navController = navController,
                                startDestination = if (deviceType == DeviceType.AIR) UpdateAir else UpdateTag
                            ) {
                                UpdateAir(
                                    scaffoldState = scaffoldState,
                                    navController = navController,
                                    viewModel = viewModelAir
                                )

                                composable<UpdateTag> {
                                    DfuUpdateScreen(viewModel, ::selectFile)
                                }
                            }
                        }
                    } else {
                        DfuUpdateScreen(viewModel, ::selectFile)
                    }
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.permissionsGranted) requestPermission()
    }

    private fun requestPermission() {
        permissionsInteractor.requestPermissions(false, true)
        val permissionsGranted = permissionsInteractor.arePermissionsGranted()
        viewModel.permissionsChecked(permissionsGranted)
        viewModelAir.permissionsChecked(permissionsGranted)
    }

    companion object {
        const val SENSOR_ID = "SENSOR_ID"

        fun start(context: Context, sensorId: String) {
            val intent = Intent(context, DfuUpdateActivity::class.java)
            intent.putExtra(SENSOR_ID, sensorId)
            context.startActivity(intent)
        }
    }
}

@Composable
fun DfuUpdateScreen(viewModel: DfuUpdateViewModel, selectFile: ()-> Unit) {

    val stage: DfuUpdateStage by viewModel.stage.observeAsState(DfuUpdateStage.CHECKING_CURRENT_FW_VERSION)
    val activity = LocalActivity.current

    Body(activity?.title.toString()) {
        when (stage) {
            DfuUpdateStage.CHECKING_CURRENT_FW_VERSION -> CheckingCurrentFwStageScreen(
                viewModel
            )

            DfuUpdateStage.DOWNLOADING_FW -> DownloadingFwStageScreen(viewModel)
            DfuUpdateStage.ALREADY_LATEST_VERSION -> AlreadyLatestVersionScreen(
                viewModel
            )

            DfuUpdateStage.READY_FOR_UPDATE -> ReadyForUpdateScreen(viewModel)
            DfuUpdateStage.UPDATE_FINISHED -> UpdateSuccessfulScreen(viewModel)
            DfuUpdateStage.UPDATING_FW -> UpdatingFwStageScreen(viewModel)
            DfuUpdateStage.ERROR -> ErrorScreen(viewModel)
        }
    }
}

@Composable
fun Body(
    title: String,
    content: @Composable () -> Unit
) {
    val systemUiController = rememberSystemUiController()

    Surface(
        color = RuuviStationTheme.colors.background,
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
    ) {
        Column() {
            RuuviTopAppBar(title)
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(RuuviStationTheme.dimensions.screenPadding)
            ) {
                content()
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

@Composable
fun CheckingCurrentFwStageScreen(viewModel: DfuUpdateViewModel) {
    val sensorFW by viewModel.sensorFwVersion.observeAsState()
    val latestFW by viewModel.latestFwVersion.observeAsState()
    val canUpdate by viewModel.canStartUpdate.observeAsState()
    val lowBattery by viewModel.lowBattery.observeAsState()

    SubtitleWithPadding(stringResource(R.string.latest_available_fw))
    if (latestFW.isNullOrEmpty()) {
        LoadingStatus(modifier = Modifier.padding(vertical = RuuviStationTheme.dimensions.textTopPadding))
    } else {
        ParagraphWithPadding("${latestFW}")
    }
    SubtitleWithPadding(stringResource(R.string.current_version_fw))
    if (sensorFW == null) {
        LoadingStatus(modifier = Modifier.padding(vertical = RuuviStationTheme.dimensions.textTopPadding))
    } else {
        if (sensorFW?.isSuccess == true) {
            ParagraphWithPadding("${sensorFW?.fw}")
        } else {
            ParagraphWithPadding(stringResource(id = R.string.old_sensor_fw))
        }
    }
    val context = LocalContext.current

    if (lowBattery == true) {
        WarningWithPadding(text = stringResource(id = R.string.dfu_low_battery_warning))
    }

    if (canUpdate == true) {
        Row(horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
            RuuviButton(
                text = stringResource(id = R.string.start_update_process),
                modifier = Modifier
                    .padding(top = RuuviStationTheme.dimensions.extraBig)
            ) {
                viewModel.startDownloadProcess(context.cacheDir)
            }
        }
    }
}

@Composable
fun DownloadingFwStageScreen(viewModel: DfuUpdateViewModel) {
    val downloadPercent by viewModel.downloadFwProgress.observeAsState()
    SubtitleWithPadding(text = stringResource(id = R.string.downloading_latest_firmware))
    Progress(progress = (downloadPercent?.toFloat() ?: 0f) / 100f)
    Row(horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        ParagraphWithPadding(text = "$downloadPercent %")
    }
}

@Composable
fun AlreadyLatestVersionScreen(viewModel: DfuUpdateViewModel) {
    ParagraphWithPadding(text = stringResource(id = R.string.already_latest_version))
}

@Composable
fun ReadyForUpdateScreen(viewModel: DfuUpdateViewModel) {
    val deviceDiscovered by viewModel.deviceDiscovered.observeAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = rememberResourceUri(resourceId = R.drawable.ruuvitag_button_location),
            contentDescription = "",
            modifier = Modifier
                .width(screenWidth * 4 / 5)
                .padding(vertical = RuuviStationTheme.dimensions.extended)
        )
    }

    MarkupText(R.string.prepare_your_sensor_instructions)

    val buttonCaption = if (deviceDiscovered == true) {
        stringResource(id = R.string.start_the_update)
    } else {
        stringResource(id = R.string.searching_for_sensor)
    }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = RuuviStationTheme.dimensions.medium)
    )
    {
        RuuviButton(
            text = buttonCaption,
            enabled = deviceDiscovered == true,
            loading = deviceDiscovered != true,
            modifier = Modifier
                .padding(top = RuuviStationTheme.dimensions.extended)
        ) {
            viewModel.startUpdateProcess()
        }
    }
}

@Composable
fun UpdateSuccessfulScreen(viewModel: DfuUpdateViewModel) {
    SubtitleWithPadding(text = stringResource(id = R.string.update_successful_tag))
}

@Composable
fun UpdatingFwStageScreen(viewModel: DfuUpdateViewModel) {
    val updatePercent by viewModel.updateFwProgress.observeAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        SubtitleWithPadding(text = stringResource(id = R.string.updating))
        Progress(progress = (updatePercent?.toFloat() ?: 0f) / 100f)

        ParagraphWithPadding(text = "$updatePercent %")
        SubtitleWithPadding(text = stringResource(id = R.string.dfu_update_do_not_close_app))
    }
}

@Composable
fun ErrorScreen(viewModel: DfuUpdateViewModel) {
    val errorMessage by viewModel.error.observeAsState()
    val errorMessageId by viewModel.errorCode.observeAsState()
    SubtitleWithPadding(text = stringResource(id = R.string.error))
    if (errorMessageId != null) {
        ParagraphWithPadding(text = stringResource(id = errorMessageId ?: R.string.unknown_error))
    } else {
        ParagraphWithPadding(text = errorMessage ?: stringResource(id = R.string.unknown_error))
    }
}

@Composable
fun LoadingStatus(modifier: Modifier = Modifier, color: Color = RuuviStationTheme.colors.accent) {
    var currentRotation by remember { mutableStateOf(0f) }
    val rotation = remember { Animatable(currentRotation) }

    LaunchedEffect(1) {
        rotation.animateTo(
            targetValue = currentRotation + 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(3000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Icon(
        Icons.Default.Refresh,
        contentDescription = "",
        modifier = modifier
            .size(24.dp)
            .rotate(rotation.value),
        tint = color
    )
}