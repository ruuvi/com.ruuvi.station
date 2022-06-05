package com.ruuvi.station.widgets.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.Paragraph

@Composable
fun WidgetConfigTopAppBar(
    viewModel: ICloudWidgetViewModel,
    title: String
) {
    val context = LocalContext.current as Activity
    val readyToBeSaved by viewModel.canBeSaved.observeAsState()

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
            if (readyToBeSaved == true) {
                TextButton(
                    onClick = { viewModel.save() }
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
fun LogInFirstScreen() {
    Column() {
        Paragraph(text = stringResource(id = R.string.widgets_sign_in_first))
        Paragraph(text = stringResource(id = R.string.widgets_gateway_only))
    }
}

@Composable
fun ForNetworkSensorsOnlyScreen() {
    Column() {
        Paragraph(text = stringResource(id = R.string.widgets_gateway_only))
    }
}

interface ICloudWidgetViewModel {
    val canBeSaved: LiveData<Boolean>
    val userLoggedIn: LiveData<Boolean>
    val userHasCloudSensors: LiveData<Boolean>
    fun save()
}
