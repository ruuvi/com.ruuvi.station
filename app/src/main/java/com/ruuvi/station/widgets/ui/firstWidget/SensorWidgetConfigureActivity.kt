package com.ruuvi.station.widgets.ui.firstWidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import com.ruuvi.station.dfu.ui.ui.theme.ComruuvistationTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ruuvi.station.R
import com.ruuvi.station.dfu.ui.RegularText
import com.ruuvi.station.tag.domain.RuuviTag
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import com.ruuvi.station.util.extensions.viewModel
import timber.log.Timber

class SensorWidgetConfigureActivity : AppCompatActivity(), KodeinAware {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override val kodein: Kodein by closestKodein()

    private val viewModel: SensorWidgetConfigureViewModel by viewModel()

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        setResult(RESULT_CANCELED)

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
                WidgetSetupScreen(viewModel)
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
            AppWidgetManager.getInstance(this@SensorWidgetConfigureActivity)

        updateAppWidget(this@SensorWidgetConfigureActivity, appWidgetManager, appWidgetId)

        val resultValue =
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

@Composable
fun WidgetSetupScreen(viewModel: SensorWidgetConfigureViewModel) {
    val sensors by viewModel.sensors.observeAsState(listOf())
    val userLoggedIn by viewModel.userLoggedIn.observeAsState(false)

    Surface(color = MaterialTheme.colors.background) {
        if (!userLoggedIn) {
            LogInFirstScreen(viewModel)
        } else if (sensors.isNullOrEmpty()) {
            ForNetworkSensorsOnlyScreen(viewModel)
        } else {
            SelectSensorScreen(viewModel)
        }
    }
}

@Composable
fun LogInFirstScreen(viewModel: SensorWidgetConfigureViewModel) {
    Column() {
        RegularText(text = stringResource(id = R.string.widgets_sign_in_first))
        RegularText(text = stringResource(id = R.string.widgets_gateway_only))
    }
}

@Composable
fun ForNetworkSensorsOnlyScreen(viewModel: SensorWidgetConfigureViewModel) {
    Column() {
        RegularText(text = stringResource(id = R.string.widgets_gateway_only))
    }
}

@Composable
fun SelectSensorScreen(viewModel: SensorWidgetConfigureViewModel) {
    val sensors by viewModel.sensors.observeAsState(listOf())
    val painter = painterResource(id = R.drawable.bg1)

    Column() {
        HeaderText("Select sensor")
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            itemsIndexed(items = sensors) {index, item ->
                SensorCard(sensor = item, painter = painter) { sensorId ->
                    Timber.d("Card clicked $sensorId")
                    viewModel.saveWidgetSettings(sensorId)
                }
            }
        }
    }
}

@Composable
fun SensorCard(
    sensor: RuuviTag,
    painter: Painter,
    action: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clickable { action(sensor.id) },
        shape = RoundedCornerShape(16.dp),
        elevation = 4.dp
    ) {
        Box(modifier = Modifier.height(100.dp)) {
            Image(
                painter = painter,
                contentDescription = sensor.displayName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black
                            ), startY = 70f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Text(text = sensor.displayName, style = TextStyle(color = Color.White, fontSize = 24.sp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComruuvistationTheme {

    }
}

@Composable
fun HeaderText(text: String) {
    Text(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        fontWeight = FontWeight.Bold,
        text = text)
}