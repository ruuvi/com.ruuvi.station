package com.ruuvi.station.widgets.ui.simpleWidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ruuvi.station.R
import com.ruuvi.station.dfu.ui.RegularText
import com.ruuvi.station.dfu.ui.ui.theme.ComruuvistationTheme
import com.ruuvi.station.dfu.ui.ui.theme.LightColorPalette
import com.ruuvi.station.util.extensions.viewModel
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.firstWidget.*
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
        WindowCompat.setDecorFitsSystemWindows(window, true)

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
                    MyTopAppBar(viewModel, title = stringResource(id = R.string.select_sensor))
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

        updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue =
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun MyTopAppBar(
    viewModel: SimpleWidgetConfigureViewModel,
    title: String
) {
    val context = LocalContext.current as Activity
    val selectedOption by viewModel.sensorId.observeAsState()

    TopAppBar(
        modifier = Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF168EA7), Color(0xFF2B486A)))),
        title = {
            Text(text = title)
        },
        navigationIcon = {
            IconButton(onClick = {
                context.onBackPressed()
            }) {
                Icon(Icons.Default.ArrowBack, stringResource(id = R.string.back))
            }
        },
        backgroundColor = Color.Transparent,
        contentColor = Color.White,
        elevation = 0.dp,
        actions = {
            if (selectedOption != null) {
                TextButton(
                    onClick = { viewModel.saveSettings() }
                ) {
                    Text(
                        color = Color.White,
                        text = stringResource(id = R.string.done)
                    )
                }
            }
        }
    )
}

@Composable
fun WidgetSetupScreen(viewModel: SimpleWidgetConfigureViewModel) {
    val systemUiController = rememberSystemUiController()
    val sensors by viewModel.cloudSensors.observeAsState(listOf())
    val userLoggedIn by viewModel.userLoggedIn.observeAsState(false)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        if (!userLoggedIn) {
            LogInFirstScreen(viewModel)
        } else if (sensors.isNullOrEmpty()) {
            ForNetworkSensorsOnlyScreen(viewModel)
        } else {
            SelectSensorScreen(viewModel)
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
fun LogInFirstScreen(viewModel: SimpleWidgetConfigureViewModel) {
    Column() {
        RegularText(text = stringResource(id = R.string.widgets_sign_in_first))
        RegularText(text = stringResource(id = R.string.widgets_gateway_only))
    }
}

@Composable
fun ForNetworkSensorsOnlyScreen(viewModel: SimpleWidgetConfigureViewModel) {
    Column() {
        RegularText(text = stringResource(id = R.string.widgets_gateway_only))
    }
}

@Composable
fun SelectSensorScreen(viewModel: SimpleWidgetConfigureViewModel) {
    val sensors by viewModel.cloudSensors.observeAsState(listOf())
    val gotFilteredSensors by viewModel.gotFilteredSensors.observeAsState(false)
    val selectedOption by viewModel.sensorId.observeAsState()

    Column() {
        if (gotFilteredSensors) {
            RegularText(text = stringResource(id = R.string.widgets_missing_sensors))
        }

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            itemsIndexed(items = sensors) {index, item ->
                SensorCard(title = item.displayName, sensorId = item.id, viewModel = viewModel, isSelected = item.id == selectedOption)
            }
        }
    }
}

@Composable
fun SensorCard(viewModel: SimpleWidgetConfigureViewModel, title: String, sensorId: String, isSelected: Boolean) {
    Column() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    modifier = Modifier
                        .padding(vertical = 8.dp),
                    selected = (isSelected),
                    onClick = { viewModel.selectSensor(sensorId) }
                )

                ClickableText(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    text = AnnotatedString(title),
                    onClick = {
                        viewModel.selectSensor(sensorId)
                    })
            }

        }
        if (isSelected) {
            WidgetTypeList(viewModel)
        }
    }
}

@Composable
fun WidgetTypeList(viewModel: SimpleWidgetConfigureViewModel) {
    val selectedOption by viewModel.widgetType.observeAsState()

    Column() {
        Row() {
            Spacer(modifier = Modifier.width(32.dp))
            Text(text = stringResource(id = R.string.widgets_select_sensor_value_type))
        }
        for (item in WidgetType.values()) {
            WidgetTypeItem(viewModel, item ,selectedOption == item)
        }
    }
}

@Composable
fun WidgetTypeItem (viewModel: SimpleWidgetConfigureViewModel, widgetType: WidgetType, isSelected: Boolean) {
    Box ( modifier = Modifier
        .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(32.dp))

            RadioButton(
                modifier = Modifier
                    .padding(vertical = 8.dp),
                selected = (isSelected),
                onClick = { viewModel.selectWidgetType(widgetType) })

            ClickableText(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                text = AnnotatedString(stringResource(id = widgetType.titleResId)),
                onClick = {
                    viewModel.selectWidgetType(widgetType)
                })
        }
    }
}