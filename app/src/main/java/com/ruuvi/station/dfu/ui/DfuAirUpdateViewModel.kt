package com.ruuvi.station.dfu.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.bluetooth.AirFirmwareInteractor
import com.ruuvi.station.bluetooth.domain.SensorInfoInteractor
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.dfu.data.DownloadFileStatus
import com.ruuvi.station.dfu.data.UploadFirmwareStatus
import com.ruuvi.station.dfu.domain.FirmwareRepository
import com.ruuvi.station.dfu.domain.downloadToFileWithProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import net.swiftzer.semver.SemVer
import timber.log.Timber
import java.io.File

class DfuAirUpdateViewModel(
    val sensorId: String,
    val firmwareRepository: FirmwareRepository,
    val preferences: PreferencesRepository,
    private val sensorInfoInteractor: SensorInfoInteractor,
    private val airFirmwareInteractor: AirFirmwareInteractor,
    private val sensorSettingsRepository: SensorSettingsRepository,
): ViewModel() {

    var fwFile: File? = null

    private val _uiEvent = MutableSharedFlow<UiEvent> (1)
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    @Volatile
    private var getFwJob: Job? = null

    private val _sensorFwVersion = MutableStateFlow<SensorFirmwareResult?>(null)
    val sensorFwVersion: StateFlow<SensorFirmwareResult?> = _sensorFwVersion

    val devMode = preferences.isDeveloperSettingsEnabled()

    private val _fwOptions = MutableStateFlow<List<FirmwareVersionOption>?>(listOf())
    val fwOptions: StateFlow<List<FirmwareVersionOption>?> = _fwOptions

    private val _selectedOption = MutableStateFlow<FirmwareVersionOption?>(null)
    val selectedOption: StateFlow<FirmwareVersionOption?> = _selectedOption

    private var _optionConfirmed =  MutableStateFlow<Boolean>(false)

    private var _connectResult = MutableStateFlow<Boolean>(false)

    init {
        getServerFirmwares()

        combine(_sensorFwVersion, _selectedOption, _fwOptions, _optionConfirmed)
        { sensorVersion, selectedOption, fwOptions, optionConfirmed ->
            if (devMode) {
                if ((fwOptions?.size ?: 0) > 1) {
                    return@combine (selectedOption != null && optionConfirmed)
                } else {
                    if (selectedOption != null && sensorVersion != null) {
                        if (sensorVersion.isSuccess) {
                            val sensorFwFirstNumberIndex =
                                sensorVersion.fw.indexOfFirst { it.isDigit() }
                            val sensorFwParsed = SemVer.parse(
                                sensorVersion.fw.subSequence(
                                    sensorFwFirstNumberIndex,
                                    sensorVersion.fw.length
                                ).toString()
                            )
                            val latestFwFirstNumberIndex =
                                selectedOption.version.indexOfFirst { it.isDigit() }
                            val latestFwParsed = SemVer.parse(
                                selectedOption.version.subSequence(
                                    latestFwFirstNumberIndex,
                                    selectedOption.version.length
                                ).toString()
                            )
                            if (sensorFwParsed.compareTo(latestFwParsed) < 0) {
                                return@combine true
                            } else {
                                _uiEvent.emit(UiEvent.NavigateNew(AlreadyUpdated, true))
                                return@combine false
                            }
                        } else {
                            return@combine true
                        }
                    } else {
                        return@combine false
                    }
                }

            } else {
                if (selectedOption != null && sensorVersion != null) {
                    if (sensorVersion.isSuccess) {
                        val sensorFwFirstNumberIndex = sensorVersion.fw.indexOfFirst { it.isDigit() }
                        val sensorFwParsed = SemVer.parse(
                            sensorVersion.fw.subSequence(sensorFwFirstNumberIndex, sensorVersion.fw.length).toString())
                        val latestFwFirstNumberIndex = selectedOption.version.indexOfFirst { it.isDigit() }
                        val latestFwParsed = SemVer.parse(
                            selectedOption.version.subSequence(latestFwFirstNumberIndex, selectedOption.version.length).toString()
                        )
                        if (sensorFwParsed.compareTo(latestFwParsed) < 0) {
                            return@combine true
                        } else {
                            //goto updated
                            _uiEvent.emit(UiEvent.NavigateNew(AlreadyUpdated, true))
                            return@combine false
                        }
                    } else {
                        return@combine true
                    }

                } else {
                    return@combine false
                }
            }
        }
            .distinctUntilChanged()
            .filter { it }
            .take(1)
            .onEach { _ ->
                _uiEvent.emit(UiEvent.NavigateNew(UpdateAirDownload, true))
            }
            .launchIn(viewModelScope)
    }

    fun getSensorFirmwareVersion() {
        if (getFwJob != null && getFwJob?.isActive == true) {
            Timber.d("Already in sync mode")
            return
        }

        getFwJob = viewModelScope.launch {
            try {
                withTimeout(60_000) {
                    Timber.d("getSensorFirmwareVersion")
                    val firmware = sensorInfoInteractor.getSensorFirmwareVersion(sensorId)
                    _sensorFwVersion.value = firmware
                    if (firmware.isSuccess && firmware.fw.isNotEmpty()) {
                        sensorSettingsRepository.setSensorFirmware(sensorId, firmware.fw)
                    }
                }
            } catch (e: TimeoutCancellationException) {
                _sensorFwVersion.value = SensorFirmwareResult(
                    isSuccess = false,
                    fw = "",
                    error = "timeout",
                    id = ""
                )
            }
        }
    }

    fun getServerFirmwares() {
        viewModelScope.launch {
            val options = firmwareRepository.getOptions()
            _fwOptions.value = options
            val latest = options?.firstOrNull { it is FirmwareVersionOption.Latest }
            _selectedOption.value = latest
        }
    }

    fun selectOption(option: FirmwareVersionOption) {
        _selectedOption.value = option
    }

    fun confirmOption() {
        _optionConfirmed.value = true
    }

    fun startDownload(destination: File): Flow<DownloadFileStatus> = flow {
        emit(DownloadFileStatus.Progress(0))
        val fwInfo = _selectedOption.value

        if (fwInfo == null) {
            emit(DownloadFileStatus.Failed("Failed to retrive filename"))
            return@flow
        }

        val localFile = File(destination, fwInfo.fileName)

        val fileUrl = fwInfo.url + "/" + fwInfo.fileName

        emitAll(firmwareRepository.getFile(fileUrl).downloadToFileWithProgress(localFile.absolutePath))
        fwFile = localFile
        emit(DownloadFileStatus.Finished(localFile))
        _connectResult.value = airFirmwareInteractor.connect(sensorId)
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

    fun permissionsChecked(permissionsGranted: Boolean) {
        if (permissionsGranted) {
            getSensorFirmwareVersion()
        }
    }
}


sealed class FirmwareVersionOption (
    val label: String,
    val version: String,
    val url: String,
    val created_at: String,
    val versionCode: Int,
    val fileName: String
){
    class Latest(
        version: String,
        url: String,
        created_at: String,
        versionCode: Int,
        fileName: String
    ): FirmwareVersionOption("Latest", version, url, created_at, versionCode, fileName)

    class Alpha(
        version: String,
        url: String,
        created_at: String,
        versionCode: Int,
        fileName: String
    ): FirmwareVersionOption("Alpha", version, url, created_at, versionCode, fileName)

    class Beta(
        version: String,
        url: String,
        created_at: String,
        versionCode: Int,
        fileName: String
    ): FirmwareVersionOption("Beta", version, url, created_at, versionCode, fileName)
}