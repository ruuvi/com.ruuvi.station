package com.ruuvi.station.dfu.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import timber.log.Timber

class DfuUpdateActivity : AppCompatActivity() , KodeinAware {

    override val kodein: Kodein by closestKodein()

    private val viewModel: DfuUpdateViewModel by viewModel { TagSettingsViewModelArgs(intent.getStringExtra(SENSOR_ID) ?:"") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val sensorId = intent.getStringExtra(SENSOR_ID)
        sensorId?.let {
            setContent {
                DfuUpdateScreen(viewModel)
            }
        }
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
        Surface(color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()) {
            Column {
                MyTopAppBar(activity.title.toString())
                when (stage) {
                    DfuUpdateStage.CHECKING_CURRENT_FW_VERSION -> CheckingCurrentFwStageScreen(viewModel)
                    DfuUpdateStage.DOWNLOADING_FW -> DownloadingFwStageScreen(viewModel)
                    DfuUpdateStage.ALREADY_LATEST_VERSION -> AlreadyLatestVersionScreen(viewModel)
                    DfuUpdateStage.READY_FOR_UPDATE -> ReadyForUpdateScreen(viewModel)
                    DfuUpdateStage.UPDATE_FINISHED -> UpdateSuccessfulScreen(viewModel)
                    DfuUpdateStage.UPDATING_FW -> TODO()
                    DfuUpdateStage.ERROR -> TODO()
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
fun ReadyForUpdateScreen(viewModel: DfuUpdateViewModel) {
    HeaderText(text = stringResource(id = R.string.prepare_your_sensor))
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

    HeaderText(stringResource(R.string.latest_available_fw))
    if (latestFW.isNullOrEmpty()) {
        LoadingStatus()
    } else {
        RegularText("${latestFW}")
    }
    HeaderText(stringResource(R.string.current_version_fw))
    
    if (sensorFW == null) {
        LoadingStatus()
    } else {
        if (sensorFW?.isSuccess == true) {
            RegularText("${sensorFW?.fw}")
        } else {
            RegularText(stringResource(id = R.string.old_sensor_fw))
        }
    }
    val context = LocalContext.current as Activity

    if (canUpdate == true) {
        Row(horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()) {
            RuuviButton(stringResource(id = R.string.start_update_process)) {
                viewModel.startUpdateProcess(context.filesDir)
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
fun RuuviButton(text: String, onClick: () -> Unit) {
    Button(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),

        shape = RoundedCornerShape(50),
        onClick = { onClick() }) {
        Text(text = text)
    }
}

@Composable
fun LoadingStatus() {
    val isRotated = remember { mutableStateOf(true) }

    val angle: Float by animateFloatAsState(
        targetValue = 360F,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse
        )
    )

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
        Modifier
            .padding(start = 16.dp, top = 16.dp)
            .size(24.dp)
            .rotate(rotation.value),
        tint = Color.Black
    )
}