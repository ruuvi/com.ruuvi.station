package com.ruuvi.station.widgets.ui.simpleWidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.ruuviRadioButtonColors
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.app.ui.theme.RuuviTheme
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.data.WidgetType.Companion.filterWidgetTypes
import com.ruuvi.station.widgets.ui.AddSensorsFirstScreen
import com.ruuvi.station.widgets.ui.EnableBackgroundService
import com.ruuvi.station.widgets.ui.WidgetConfigTopAppBar
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import timber.log.Timber

class SimpleWidgetConfigureActivity : AppCompatActivity(), KodeinAware {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override val kodein: Kodein by closestKodein()

    private val viewModel: SimpleWidgetConfigureViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

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
                Column(modifier = Modifier.systemBarsPadding()) {
                    WidgetConfigTopAppBar(viewModel, title = stringResource(id = R.string.select_sensor))
                    WidgetSetupScreen(viewModel)
                }
            }
        }
    }

    fun setupViewModel() {
        viewModel.setWidgetId(appWidgetId)

        viewModel.setupComplete.observe(this) { setupComplete ->
            Timber.d("setupComplete $setupComplete appWidgetId $appWidgetId")
            if (setupComplete) setupCompleted()
        }
    }

    private fun setupCompleted() {
        val appWidgetManager =
            AppWidgetManager.getInstance(this)

        SimpleWidget.updateSimpleWidget(this, appWidgetManager, appWidgetId)

        val resultValue =
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetSetupScreen(viewModel: SimpleWidgetConfigureViewModel) {
    val sensors by viewModel.allSensors.observeAsState(listOf())

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RuuviStationTheme.colors.background,
    ) {
        if (sensors.isNullOrEmpty()) {
            AddSensorsFirstScreen()
        } else {
            SelectSensorScreen(viewModel)
        }
    }
}

@Composable
fun SelectSensorScreen(viewModel: SimpleWidgetConfigureViewModel) {
    val sensors by viewModel.allSensors.observeAsState(listOf())
    val gotLocalSensors by viewModel.gotLocalSensors.observeAsState(false)
    val selectedOption by viewModel.sensorId.observeAsState()
    val backgroundServiceEnabled by viewModel.backgroundServiceEnabled.observeAsState(true)

    LazyColumn() {
        item {
            if (gotLocalSensors && !backgroundServiceEnabled) {
                EnableBackgroundService(viewModel.backgroundServiceInterval) {
                    viewModel.enableBackgroundService()
                }
            }
        }

        itemsIndexed(items = sensors) { _, item ->
            SensorItem(
                sensor = item,
                viewModel = viewModel,
                isSelected = item.id == selectedOption
            )
        }
    }
}

@Composable
fun SensorItem(
    sensor: RuuviTag,
    viewModel: SimpleWidgetConfigureViewModel,
    isSelected: Boolean
) {
    Column() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (isSelected),
                    colors = ruuviRadioButtonColors(),
                    onClick = { viewModel.selectSensor(sensor.id) }
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectSensor(sensor.id) },
                    text = sensor.displayName,
                    style = RuuviStationTheme.typography.paragraph
                )
            }

        }
        if (isSelected) {
            WidgetTypeList(sensor, viewModel)
        }
    }
}

@Composable
fun WidgetTypeList(
    sensor: RuuviTag,
    viewModel: SimpleWidgetConfigureViewModel
) {
    val selectedOption by viewModel.widgetType.observeAsState()

    Column(modifier = Modifier.padding(start = RuuviStationTheme.dimensions.extraBig)) {
        Row() {
            Paragraph(
                text = stringResource(id = R.string.widgets_select_sensor_value_type),
                modifier = Modifier.padding(RuuviStationTheme.dimensions.medium)
            )
        }
        for (item in filterWidgetTypes(sensor)) {
            WidgetTypeItem(viewModel, item ,selectedOption == item)
        }
    }
}

@Composable
fun WidgetTypeItem (viewModel: SimpleWidgetConfigureViewModel, widgetType: WidgetType, isSelected: Boolean) {
    Box (
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = (isSelected),
                colors = ruuviRadioButtonColors(),
                onClick = { viewModel.selectWidgetType(widgetType) })

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.selectWidgetType(widgetType) },
                text = stringResource(id = widgetType.titleResId),
                style = RuuviStationTheme.typography.paragraph)
        }
    }
}