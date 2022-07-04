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
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.ruuviCheckboxColors
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
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
            RuuviTheme {
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
    val userHasCloudSensors by viewModel.userHasCloudSensors.observeAsState(false)
    val userLoggedIn by viewModel.userLoggedIn.observeAsState(false)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RuuviStationTheme.colors.background
    ) {
        if (!userLoggedIn) {
            LogInFirstScreen()
        } else if (userHasCloudSensors) {
            SelectSensorsScreen(viewModel)
        } else {
            ForNetworkSensorsOnlyScreen()
        }
    }
}

@Composable
fun SelectSensorsScreen(viewModel: ComplexWidgetConfigureViewModel) {
    val sensors by viewModel.widgetItems.observeAsState(listOf())
    val gotFilteredSensors by viewModel.gotFilteredSensors.observeAsState(false)

    LazyColumn() {
        item {
            if (gotFilteredSensors) {
                Paragraph(
                    text = stringResource(id = R.string.widgets_missing_sensors),
                    modifier = Modifier.padding(RuuviStationTheme.dimensions.screenPadding)
                )
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
                    colors = ruuviCheckboxColors(),
                    onCheckedChange = { checked -> viewModel.selectSensor(item, checked)},
                )

                ClickableText(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = AnnotatedString(item.sensorName),
                    style = RuuviStationTheme.typography.paragraph,
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
    Column(modifier = Modifier.padding(start = RuuviStationTheme.dimensions.extraBig)) {
        Row() {
            Paragraph(
                text = stringResource(id = R.string.widgets_select_sensor_value_type),
                modifier = Modifier.padding(RuuviStationTheme.dimensions.medium)
            )
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
            Checkbox(
                checked = item.getStateForType(widgetType),
                colors = ruuviCheckboxColors(),
                onCheckedChange = { checked -> viewModel.selectWidgetType(item, widgetType, checked) })

            ClickableText(
                modifier = Modifier
                    .fillMaxWidth(),
                text = AnnotatedString(stringResource(id = widgetType.titleResId)),
                style = RuuviStationTheme.typography.paragraph,
                onClick = { viewModel.selectWidgetType(item, widgetType, item.getStateForType(widgetType)) }
            )
        }
}