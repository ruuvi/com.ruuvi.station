package com.ruuvi.station.dfu.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.components.MarkupText
import com.ruuvi.station.app.ui.components.ParagraphWithPadding
import com.ruuvi.station.app.ui.components.Progress
import com.ruuvi.station.app.ui.components.RadioButtonRuuvi
import com.ruuvi.station.app.ui.components.RuuviButton
import com.ruuvi.station.app.ui.components.SubtitleWithPadding
import com.ruuvi.station.app.ui.theme.RuuviStationTheme
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.dfu.data.DownloadFileStatus
import com.ruuvi.station.dfu.data.UploadFirmwareStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.io.File

fun NavGraphBuilder.UpdateAir(
    scaffoldState: ScaffoldState,
    navController: NavHostController,
    viewModel: DfuAirUpdateViewModel
) {
    this.navigation<UpdateAir>(
        startDestination = CheckFirmwareVersion,
    ) {
        composable<CheckFirmwareVersion> {
            val sensorFW by viewModel.sensorFwVersion.collectAsState()
            val selectedFw by viewModel.selectedOption.collectAsState()
            val fwOptions by viewModel.fwOptions.collectAsState()
            val devMode = viewModel.devMode
            //val latestFW by viewModel.latestFwVersion.observeAsState()

            CheckFirmwareVersion(
                sensorFW = sensorFW,
                selectedFw = selectedFw,
                fwOptions = fwOptions,
                devMode = devMode,
                modifier = Modifier,
                selectFw = viewModel::selectOption,
                confirmOption = viewModel::confirmOption
            )
        }

        composable<UpdateAirDownload> {
            DownloadFirmwareScreen(
                navController = navController,
                startDownload = viewModel::startDownload
            )
        }
        composable<UpdateAirInstructions> {
            FirmwareInstructionsScreen(
                navController = navController
            )
        }

        composable<UpdateAirUploadFirmware> {
            UploadFirmwareScreen(
                navController = navController,
                uploadFw = viewModel::upload
            )
        }

        composable<UpdateAirSuccess> {
            UpdateSuccessScreen(navController)
        }

        composable<AlreadyUpdated> {
            AlreadyUpdatedScreen(navController)
        }

        composable<UpdateFailed> {
            UpdateFailedScreen(navController)
        }
    }
}

@Composable
fun CheckFirmwareVersion(
    sensorFW: SensorFirmwareResult?,
    selectedFw: FirmwareVersionOption?,
    fwOptions: List<FirmwareVersionOption>?,
    devMode: Boolean,
    selectFw: (FirmwareVersionOption) -> Unit,
    confirmOption: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(RuuviStationTheme.dimensions.screenPadding)
    ) {
        SubtitleWithPadding(stringResource(R.string.latest_available_fw))
        if (selectedFw == null) {
            LoadingStatus(modifier = Modifier.padding(vertical = RuuviStationTheme.dimensions.textTopPadding))
        } else {
            if (devMode && fwOptions != null && (fwOptions?.size ?: 0) > 1) {
                for (fw in fwOptions) {
                    RadioButtonRuuvi("${fw.label} ${fw.version}" , isSelected = fw == selectedFw) {
                        selectFw(fw)
                    }
                }
                Spacer(Modifier.height(16.dp))
                RuuviButton("Proceed"
                ) {
                    confirmOption.invoke()
                }
                Spacer(Modifier.height(16.dp))
            } else {
                ParagraphWithPadding(selectedFw.version)
            }
        }
        SubtitleWithPadding(stringResource(R.string.current_version_fw))
        if (sensorFW == null) {
            LoadingStatus(modifier = Modifier.padding(vertical = RuuviStationTheme.dimensions.textTopPadding))
        } else {
            if (sensorFW?.isSuccess == true) {
                ParagraphWithPadding("${sensorFW?.fw}")
            } else {
                ParagraphWithPadding(stringResource(id = R.string.old_sensor_fw))
            }
        }

    }
}

@Composable
fun AlreadyUpdatedScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    BackHandler { activity?.finish() }

    Column(modifier =
        Modifier
            .padding(RuuviStationTheme.dimensions.screenPadding)
    ) {
        MarkupText(R.string.already_latest_version)
    }
}

@Composable
fun UpdateFailedScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val activity = LocalActivity.current
    BackHandler { activity?.finish() }

    Column(modifier =
        Modifier
            .padding(RuuviStationTheme.dimensions.screenPadding)
    ) {
        MarkupText(R.string.unknown_error)
    }
}

 @Composable
fun DownloadFirmwareScreen(
     navController: NavHostController,
     startDownload: (File) -> Flow<DownloadFileStatus>
) {
     val context = LocalContext.current

     var downloadPercent by remember { mutableStateOf(0) }

     LaunchedEffect(Unit) {
         startDownload(context.cacheDir).collectLatest {
             if (it is DownloadFileStatus.Progress) {
                 downloadPercent = it.percent
             } else if (it is DownloadFileStatus.Finished) {
                 downloadPercent = 100
                 navController.navigate(UpdateAirInstructions)
             }
             Timber.d("Collected status $it")
         }
     }

     Column(
         modifier = Modifier
             .padding(RuuviStationTheme.dimensions.screenPadding)
     ) {
         SubtitleWithPadding(text = stringResource(id = R.string.downloading_latest_firmware))
         Progress(progress = (downloadPercent.toFloat() ?: 0f) / 100f)
         Row(
             horizontalArrangement = Arrangement.Center,
             modifier = Modifier.fillMaxWidth()
         ) {
             ParagraphWithPadding(text = "$downloadPercent %")
         }
     }
 }

@Composable
fun FirmwareInstructionsScreen(
    navController: NavHostController
) {
    val activity = LocalActivity.current
    BackHandler { activity?.finish() }

    Column(modifier =
        Modifier
            .padding(RuuviStationTheme.dimensions.screenPadding)
    ) {
        MarkupText(R.string.dfu_air_update_instructions)
        Spacer(modifier = Modifier.height(64.dp))
        RuuviButton(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.start_the_update),
        ) {
            navController.navigate(UpdateAirUploadFirmware)
        }
    }
}

@Composable
fun UploadFirmwareScreen(
    navController: NavHostController,
    uploadFw: () -> Flow<UploadFirmwareStatus>
) {
    var uploadPercent by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        uploadFw.invoke().collectLatest {
            if (it is UploadFirmwareStatus.Progress) {
                uploadPercent = it.percent
            } else if (it is UploadFirmwareStatus.Finished) {
                uploadPercent = 100
                navController.navigate(UpdateAirSuccess)
            } else if (it is UploadFirmwareStatus.Failed) {
                navController.navigate(UpdateFailed)
            }
            Timber.d("Collected status $it")
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(RuuviStationTheme.dimensions.screenPadding)
            .fillMaxWidth()
    ) {
        SubtitleWithPadding(text = stringResource(id = R.string.updating))
        Progress(progress = (uploadPercent.toFloat() ?: 0f) / 100f)

        ParagraphWithPadding(text = "$uploadPercent %")
        SubtitleWithPadding(text = stringResource(id = R.string.dfu_update_do_not_close_app))
    }
}

@Composable
fun UpdateSuccessScreen(
    navController: NavHostController
) {
    val activity = LocalActivity.current
    BackHandler { activity?.finish() }

    Column(modifier =
        Modifier
            .padding(RuuviStationTheme.dimensions.screenPadding)
    ) {
        MarkupText(R.string.update_successful_air)
    }
}