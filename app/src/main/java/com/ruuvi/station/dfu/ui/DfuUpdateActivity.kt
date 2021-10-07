package com.ruuvi.station.dfu.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
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

        // A surface container using the 'background' color from the theme
        Surface(color = Color.White) {
            Column() {
                MyTopAppBar("DFU Update")
                when (stage) {
                    DfuUpdateStage.CHECKING_CURRENT_FW_VERSION -> CheckingCurrentFwStageScreen(viewModel)
                    DfuUpdateStage.READY_FOR_UPDATE -> ReadyForUpdateStageScreen(viewModel)
                }
                Text(text = "test text")
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
fun CheckingCurrentFwStageScreen(viewModel: DfuUpdateViewModel) {
    val sensorFW = viewModel.sensorFwVersion.observeAsState()

    Text(text = "CheckingCurrentFwStageScreen FW = ${sensorFW.value}")
}

@Composable
fun ReadyForUpdateStageScreen(viewModel: DfuUpdateViewModel) {
    val sensorFW = viewModel.sensorFwVersion.observeAsState()

    Text(text = "ReadyForUpdateStageScreen FW = ${sensorFW.value}")
    Button(onClick = {viewModel.startUpdate()}) {
        Text(text = "press")
    }
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