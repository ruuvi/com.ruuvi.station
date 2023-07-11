package com.ruuvi.station.tagsettings.ui

import android.content.*
import android.os.*
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.TaskStackBuilder
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.alarm.ui.AlarmItemsViewModel
import com.ruuvi.station.app.ui.RuuviTopAppBar
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
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
            TagSettingsViewModelArgs(it, intent.getBooleanExtra(NEW_SENSOR, false))
        }
    }

    private val alarmsViewModel: AlarmItemsViewModel by viewModel {
        intent.getStringExtra(TAG_ID)
    }

    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RuuviTheme {
                val context = LocalContext.current
                val scaffoldState = rememberScaffoldState()
                val systemUiController = rememberSystemUiController()
                val systemBarsColor = RuuviStationTheme.colors.systemBars
                var showExportDialog by remember {mutableStateOf(false)}

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    backgroundColor = RuuviStationTheme.colors.background,
                    topBar = { RuuviTopAppBar(
                        title = stringResource(id = R.string.sensor_settings))
                    },
                    scaffoldState = scaffoldState
                ) { paddingValues ->
                    SensorSettings(
                        scaffoldState = scaffoldState,
                        viewModel = viewModel,
                        alarmsViewModel = alarmsViewModel
                    )
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
    }
}