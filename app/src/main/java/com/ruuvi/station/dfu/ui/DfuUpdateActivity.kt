package com.ruuvi.station.dfu.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.dfu.ui.ui.theme.ComruuvistationTheme
import com.ruuvi.station.dfu.ui.ui.theme.LightColorPalette
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import com.ruuvi.station.util.extensions.viewModel
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.domain.PermissionsInteractor

class DfuUpdateActivity : AppCompatActivity() , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: DfuUpdateViewModel by viewModel { TagSettingsViewModelArgs(intent.getStringExtra(SENSOR_ID) ?:"") }

    private lateinit var permissionsInteractor: PermissionsInteractor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val sensorId = intent.getStringExtra(SENSOR_ID)
        sensorId?.let {
            setContent {
                DfuUpdateScreen(viewModel)
            }
        }
        permissionsInteractor = PermissionsInteractor(this)
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.permissionsGranted) requestPermission()
    }

    private fun requestPermission() {
        val permissionsGranted = permissionsInteractor.arePermissionsGranted()
        viewModel.permissionsChecked(permissionsGranted)
        if (!permissionsGranted) permissionsInteractor.showPermissionSnackbar()
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
fun DfuUpdateScreen(viewModel: DfuUpdateViewModel) {
    ComruuvistationTheme {
        val systemUiController = rememberSystemUiController()

        val stage: DfuUpdateStage by viewModel.stage.observeAsState(DfuUpdateStage.CHECKING_CURRENT_FW_VERSION)

        val activity = LocalContext.current as Activity

        // A surface container using the 'background' color from the theme
        Surface(
            color = Color.White,
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column (modifier = Modifier.verticalScroll(rememberScrollState()))
            {
                MyTopAppBar(activity.title.toString())
                when (stage) {
                    DfuUpdateStage.CHECKING_CURRENT_FW_VERSION -> CheckingCurrentFwStageScreen(viewModel)
                    DfuUpdateStage.DOWNLOADING_FW -> DownloadingFwStageScreen(viewModel)
                    DfuUpdateStage.ALREADY_LATEST_VERSION -> AlreadyLatestVersionScreen(viewModel)
                    DfuUpdateStage.READY_FOR_UPDATE -> ReadyForUpdateScreen(viewModel)
                    DfuUpdateStage.UPDATE_FINISHED -> UpdateSuccessfulScreen(viewModel)
                    DfuUpdateStage.UPDATING_FW -> UpdatingFwStageScreen(viewModel)
                    DfuUpdateStage.ERROR -> ErrorScreen(viewModel)
                }
            }
        }

        SideEffect {
            systemUiController.setSystemBarsColor(
                color = LightColorPalette.surface,
                darkIcons = false
            )
        }
    }
}

@Composable
fun ErrorScreen(viewModel: DfuUpdateViewModel) {
    val errorMessage by viewModel.error.observeAsState()
    val errorMessageId by viewModel.errorCode.observeAsState()
    HeaderText(text = stringResource(id = R.string.error))
    if (errorMessageId != null) {
        RegularText(text = stringResource(id = errorMessageId ?: R.string.unknown_error))
    } else {
        RegularText(text = errorMessage ?: stringResource(id = R.string.unknown_error))
    }
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
        Image(
            painter = painterResource(id = R.drawable.ruuvitag_button_location),
            contentDescription = "",
            modifier = Modifier
                .width(screenWidth / 2)
                .padding(top = 16.dp)
        )
    }

    HeaderText(text = stringResource(id = R.string.prepare_your_sensor))
    RegularText(text = stringResource(id = R.string.prepare_your_sensor_instructions_1))
    RegularText(text = stringResource(id = R.string.prepare_your_sensor_instructions_2))
    RegularText(text = stringResource(id = R.string.prepare_your_sensor_instructions_3))
    RegularText(text = stringResource(id = R.string.prepare_your_sensor_instructions_4))
    RegularText(text = stringResource(id = R.string.prepare_your_sensor_instructions_5))
    RegularText(text = stringResource(id = R.string.prepare_your_sensor_instructions_6))

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
            .padding(bottom = 8.dp)
    )
    {
        RuuviButton(text = buttonCaption, deviceDiscovered == true, deviceDiscovered != true) {
            viewModel.startUpdateProcess()
        }
    }
}

@Composable
fun UpdateSuccessfulScreen(viewModel: DfuUpdateViewModel) {
    HeaderText(text = stringResource(id = R.string.update_successful))
}

@Composable
fun CheckingCurrentFwStageScreen(viewModel: DfuUpdateViewModel) {
    val sensorFW by viewModel.sensorFwVersion.observeAsState()
    val latestFW by viewModel.latestFwVersion.observeAsState()
    val canUpdate by viewModel.canStartUpdate.observeAsState()
    val lowBattery by viewModel.lowBattery.observeAsState()

    HeaderText(stringResource(R.string.latest_available_fw))
    if (latestFW.isNullOrEmpty()) {
        LoadingStatus(modifier = Modifier.padding(top = 16.dp))
    } else {
        RegularText("${latestFW}")
    }
    HeaderText(stringResource(R.string.current_version_fw))
    
    if (sensorFW == null) {
        LoadingStatus(modifier = Modifier.padding(top = 16.dp))
    } else {
        if (sensorFW?.isSuccess == true) {
            RegularText("${sensorFW?.fw}")
        } else {
            RegularText(stringResource(id = R.string.old_sensor_fw))
        }
    }
    val context = LocalContext.current as Activity

    if (lowBattery == true) {
        WarningText(stringResource(id = R.string.dfu_low_battery_warning))
    }
    
    if (canUpdate == true) {
        Row(horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()) {
            RuuviButton(stringResource(id = R.string.start_update_process)) {
                viewModel.startDownloadProcess(context.cacheDir)
            }
        }
    }
}

@Composable
fun DownloadingFwStageScreen(viewModel: DfuUpdateViewModel) {
    val downloadPercent by viewModel.downloadFwProgress.observeAsState()
    HeaderText(text = stringResource(id = R.string.downloading_latest_firmware))
    LinearProgressIndicator(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth(),
        progress = (downloadPercent?.toFloat() ?: 0f) / 100f
    )
    Row(horizontalArrangement = Arrangement.Center,
    modifier = Modifier.fillMaxWidth()) {
        RegularText(text = "$downloadPercent %")
    }
}

@Composable
fun UpdatingFwStageScreen(viewModel: DfuUpdateViewModel) {
    val updatePercent by viewModel.updateFwProgress.observeAsState()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        HeaderText(text = stringResource(id = R.string.updating))
        LinearProgressIndicator(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth(),
            progress = (updatePercent?.toFloat() ?: 0f) / 100f
        )

        RegularText(text = "$updatePercent %")
        HeaderText(text = stringResource(id = R.string.dfu_update_do_not_close_app))
    }
}

@Composable
fun AlreadyLatestVersionScreen(viewModel: DfuUpdateViewModel) {
    RegularText(text = stringResource(id = R.string.already_latest_version))
}

@Composable
fun MyTopAppBar(
    title: String
) {
    val context = LocalContext.current as Activity

    TopAppBar(
        modifier = Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF168EA7), Color(0xFF2B486A)))),
        title = {
            Text(text = title)
        },
        navigationIcon = {
            IconButton(onClick = {
                context.onBackPressed()
            }) {
                Icon(Icons.Default.ArrowBack, "Back")
            }
        },
        backgroundColor = Color.Transparent,
        contentColor = Color.White,
        elevation = 0.dp
    )
}

@Composable
fun HeaderText(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        fontWeight = FontWeight.Bold,
        text = text)
}

@Composable
fun RegularText(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        text = text)
}

@Composable
fun WarningText(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        color = Color.Red,
        text = text)
}

@Composable
fun RuuviButton(text: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        onClick = { onClick() }) {
        Row(horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically) {
            Text(text = text)
            if (loading) LoadingStatus(Modifier.clickable {  })
        }
    }
}

@Composable
fun LoadingStatus(modifier: Modifier) {
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
        contentDescription = "Localized description",
        modifier = modifier
            .padding(start = 16.dp)
            .size(24.dp)
            .rotate(rotation.value),
        tint = Color.Black
    )
}