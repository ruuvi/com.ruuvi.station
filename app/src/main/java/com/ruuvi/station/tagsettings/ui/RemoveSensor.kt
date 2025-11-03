package com.ruuvi.station.tagsettings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ScaffoldState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.components.ExpandableContainer
import com.ruuvi.station.app.ui.components.PageSurfaceWithPadding
import com.ruuvi.station.app.ui.components.Paragraph
import com.ruuvi.station.app.ui.components.ParagraphWithPadding
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.components.RuuviCheckbox
import com.ruuvi.station.app.ui.components.dialog.CustomContentDialog
import com.ruuvi.station.app.ui.components.TextEditWithCaptionButton
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.dashboard.ui.DashboardActivity

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun RemoveGroup(
    deleteSensor: ()->Unit
) {
    ExpandableContainer(header = {
        Text(
            text = stringResource(id = R.string.remove),
            style = RuuviStationTheme.typography.title,
        )
    },
        backgroundColor = RuuviStationTheme.colors.settingsTitle
    ) {
        TextEditWithCaptionButton(
            title = stringResource(id = R.string.remove_this_sensor),
            icon = painterResource(id = R.drawable.arrow_forward_16),
            tint = RuuviStationTheme.colors.trackInactive
        ) {
            deleteSensor.invoke()
        }
    }
}

@Composable
fun RemoveSensor(
    scaffoldState: ScaffoldState,
    onNavigate: (UiEvent.Navigate) -> Unit,
    viewModel: RemoveSensorViewModel,
) {
    val sensorState by viewModel.sensorState.collectAsState()
    val sensorOwnedByUser by viewModel.sensorOwnedByUser.collectAsState(initial = false)
    val deleteDataEnabled = viewModel.removeWithCloudData.collectAsState()
    var removeDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    PageSurfaceWithPadding {
        Column {
            if (!sensorState.networkSensor) {
                ParagraphWithPadding(text = stringResource(id = R.string.remove_local_sensor_description))
            } else {
                if (sensorOwnedByUser) {
                    ParagraphWithPadding(text = stringResource(id = R.string.remove_claimed_sensor_description))

                    RuuviCheckbox(
                        checked = deleteDataEnabled.value,
                        text = stringResource(id = R.string.remove_cloud_history_description),
                        onCheckedChange = viewModel::setRemoveWithCloudData
                    )
                } else {
                    ParagraphWithPadding(text = stringResource(id = R.string.remove_shared_sensor_description))
                }
            }

            Spacer(modifier = Modifier.height(RuuviStationTheme.dimensions.big))

            Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center){
                RuuviButton(text = stringResource(id = R.string.remove)) {
                    removeDialog = true
                }
            }
        }
    }

    if (removeDialog) {
        CustomContentDialog(
            title = stringResource(id = R.string.dialog_are_you_sure),
            onDismissRequest = { removeDialog = false },
            onOkClickAction = {
                viewModel.removeSensor()
                DashboardActivity.start(context)
            },
            positiveButtonText = stringResource(id = R.string.confirm)
        ) {
            Paragraph(text = stringResource(id = R.string.dialog_operation_undone))
        }
    }
}