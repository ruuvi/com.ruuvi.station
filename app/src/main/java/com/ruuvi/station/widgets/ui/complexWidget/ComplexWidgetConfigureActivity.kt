package com.ruuvi.station.widgets.ui.complexWidget

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.app.TaskStackBuilder
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.dfu.ui.RegularText
import com.ruuvi.station.dfu.ui.ui.theme.ComruuvistationTheme
import com.ruuvi.station.dfu.ui.ui.theme.LightColorPalette
import com.ruuvi.station.tagdetails.ui.TagDetailsActivity
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.widgets.complexWidget.ComplexWidgetConfigureViewModel
import com.ruuvi.station.widgets.complexWidget.ComplexWidgetConfigureViewModelArgs
import com.ruuvi.station.widgets.complexWidget.ComplexWidgetSensorItem
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.*
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

class ComplexWidgetConfigureActivity : AppCompatActivity(), KodeinAware {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override val kodein: Kodein by closestKodein()

    private val viewModel: ComplexWidgetConfigureViewModel by viewModel() {
        appWidgetId = intent.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        ComplexWidgetConfigureViewModelArgs(appWidgetId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setupViewModel()

        setContent {
            ComruuvistationTheme {
                Column() {
                    WidgetConfigTopAppBar(viewModel, title = stringResource(id = R.string.select_sensor))
                    WidgetSetupScreen(viewModel)
                }
            }
        }
    }

    private fun setupViewModel() {
        Timber.d("setupViewModel")

        viewModel.setupComplete.observe(this) { setupComplete ->
            if (setupComplete) setupCompleted()
        }
    }

    private fun setupCompleted() {
        val updateIntent = Intent(this, ComplexWidgetProvider::class.java).apply {
            action = ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
        }
        sendBroadcast(updateIntent)

        val resultValue =
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }

    companion object {
        fun createPendingIntent(context: Context, appWidgetId: Int): PendingIntent? {
            val intent = Intent(context, ComplexWidgetConfigureActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            return PendingIntent.getActivity(context, appWidgetId, intent, FLAG_IMMUTABLE)
        }
    }
}

@Composable
fun WidgetSetupScreen(viewModel: ComplexWidgetConfigureViewModel) {
    val systemUiController = rememberSystemUiController()
    val userHasCloudSensors by viewModel.userHasCloudSensors.observeAsState(false)
    val userLoggedIn by viewModel.userLoggedIn.observeAsState(false)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        if (!userLoggedIn) {
            LogInFirstScreen()
        } else if (userHasCloudSensors) {
            SelectSensorsScreen(viewModel)
        } else {
            ForNetworkSensorsOnlyScreen()
        }
    }

    SideEffect {
        systemUiController.setSystemBarsColor(
            color = LightColorPalette.surface,
            darkIcons = false
        )
    }
}

@Composable
fun SelectSensorsScreen(viewModel: ComplexWidgetConfigureViewModel) {
    val sensors by viewModel.widgetItems.observeAsState(listOf())

    val gotFilteredSensors by viewModel.gotFilteredSensors.observeAsState(false)

    Timber.d("sensors = $sensors")

    LazyColumn() {
        item {
            if (gotFilteredSensors) {
                RegularText(text = stringResource(id = R.string.widgets_missing_sensors))
            }
        }

        if (sensors?.isNotEmpty() == true) {
            itemsIndexed(items = sensors) { _, item ->
                SensorSettingsCard(
                    viewModel = viewModel,
                    item = item
                )
            }
        }

    }
}

@Composable
fun SensorSettingsCard(viewModel: ComplexWidgetConfigureViewModel, item: ComplexWidgetSensorItem) {
    Timber.d("SensorSettingsCard $item")
    Column() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = (item.checked),
                    onCheckedChange = { checked -> viewModel.selectSensor(item, checked)},
                )

                ClickableText(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = AnnotatedString(item.sensorName),
                    onClick = {
                        viewModel.selectSensor(item, !item.checked)
                    })
            }

        }
        if (item.checked) {
            WidgetTypeList(viewModel, item)
        }
    }
}

@Composable
fun WidgetTypeList(viewModel: ComplexWidgetConfigureViewModel, item: ComplexWidgetSensorItem) {
    Timber.d("WidgetTypeList")
    Column() {
        Row() {
            Spacer(modifier = Modifier.width(32.dp))
            Text(text = stringResource(id = R.string.widgets_select_sensor_value_type))
        }


        for (widgetType in WidgetType.values()) {
            WidgetTypeItem(viewModel, item, widgetType)
        }
    }
}

@Composable
fun WidgetTypeItem (viewModel: ComplexWidgetConfigureViewModel, item: ComplexWidgetSensorItem, widgetType: WidgetType) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.wrapContentHeight()
        )
        {
            Spacer(modifier = Modifier
                .width(16.dp)
                .height(0.dp))

            Checkbox(
                checked = item.getStateForType(widgetType),
                onCheckedChange = { checked -> viewModel.selectWidgetType(item, widgetType, checked) })

            ClickableText(
                modifier = Modifier
                    .fillMaxWidth(),
                text = AnnotatedString(stringResource(id = widgetType.titleResId)),
                onClick = { viewModel.selectWidgetType(item, widgetType, item.getStateForType(widgetType)) }
            )
        }

}