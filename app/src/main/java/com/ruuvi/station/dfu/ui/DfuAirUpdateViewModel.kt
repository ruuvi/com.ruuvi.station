package com.ruuvi.station.dfu.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.bluetooth.AirFirmwareInteractor
import com.ruuvi.station.dfu.data.DownloadFileStatus
import com.ruuvi.station.dfu.data.UploadFirmwareStatus
import com.ruuvi.station.dfu.domain.FirmwareRepository
import com.ruuvi.station.dfu.domain.downloadToFileWithProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber
import java.io.File

class DfuAirUpdateViewModel(
    val sensorId: String,
    val firmwareRepository: FirmwareRepository,
    private val airFirmwareInteractor: AirFirmwareInteractor
): ViewModel() {

    var fwFile: File? = null

    fun startDownload(destination: File): Flow<DownloadFileStatus> = flow {
        emit(DownloadFileStatus.Progress(0))
        val fwInfo = firmwareRepository.getLatest()

        if (fwInfo == null) {
            emit(DownloadFileStatus.Failed("Failed to retrive filename"))
            return@flow
        }

        val localFile = File(destination, fwInfo.fileName)
        if (localFile.exists()) {
            fwFile = localFile
            airFirmwareInteractor.connect(sensorId)
            emit(DownloadFileStatus.Finished(localFile))
            return@flow
        }

        val fileUrl = fwInfo.url + "/" + fwInfo.fileName

        emitAll(firmwareRepository.getFile(fileUrl).downloadToFileWithProgress(localFile.absolutePath))
        fwFile = localFile
        emit(DownloadFileStatus.Finished(localFile))
        airFirmwareInteractor.connect(sensorId)
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()


    fun upload(): Flow<UploadFirmwareStatus> = callbackFlow  {
        Timber.d("fwFile $fwFile")
        fwFile?.let {
            airFirmwareInteractor.upload(
                it,
                progress = { current, total ->
                    trySend(UploadFirmwareStatus.Progress((current.toDouble() / total * 100).toInt()))
                },
                done = { trySend(UploadFirmwareStatus.Finished)  } ,
                fail = { error -> trySend(UploadFirmwareStatus.Failed(error)) },
            )
        }

        awaitClose {}
    }.distinctUntilChanged()
}