package com.ruuvi.station.dfu.ui

import androidx.lifecycle.*
import com.ruuvi.station.bluetooth.domain.SensorFwVersionInteractor
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.dfu.data.DownloadFileStatus
import com.ruuvi.station.dfu.data.LatestReleaseResponse
import com.ruuvi.station.dfu.domain.LatestFwInteractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import timber.log.Timber
import java.io.File

class DfuUpdateViewModel(
    val sensorId: String,
    private val sensorFwVersionInteractor: SensorFwVersionInteractor,
    private val latestFwInteractor: LatestFwInteractor,
    ): ViewModel() {

    private val _stage = MutableLiveData(DfuUpdateStage.CHECKING_CURRENT_FW_VERSION)
    val stage: LiveData<DfuUpdateStage> = _stage

    private val _sensorFwVersion = MutableLiveData<SensorFirmwareResult>(null)
    val sensorFwVersion: LiveData<SensorFirmwareResult> = _sensorFwVersion

    private val _latestFwVersion = MutableLiveData<String>(null)
    val latestFwVersion: LiveData<String> = _latestFwVersion

    private val _downloadFwProgress = MutableLiveData<Int>(0)
    val downloadFwProgress: LiveData<Int> = _downloadFwProgress

    val canStartUpdate = MediatorLiveData<Boolean>()

    private var latestFwinfo: LatestReleaseResponse? = null

    @Volatile
    private var getFwJob: Job? = null

    init {
        Timber.d("Init viewmodel for $sensorId ${sensorFwVersion.value}")
        getSensorFirmwareVersion()
        getLatestFw()

        canStartUpdate.addSource(_sensorFwVersion) { canStartUpdate.value = updateAllowed() }
        canStartUpdate.addSource(_latestFwVersion) { canStartUpdate.value = updateAllowed() }
        canStartUpdate.addSource(_stage) { canStartUpdate.value = updateAllowed() }
    }

    private fun updateAllowed(): Boolean {
        val sensorFw = _sensorFwVersion.value
        val latestFw = _latestFwVersion.value
        val stage = _stage.value

        if (sensorFw == null || latestFw == null || stage != DfuUpdateStage.CHECKING_CURRENT_FW_VERSION) {
            return false
        }

        if (!sensorFw.isSuccess) {
            return true
        } else {
            val firstNumberIndex = latestFw.indexOfFirst { it.isDigit() }
            val sensorFwParsed = SemVer.parse(sensorFw.fw)
            val latestFwParsed = SemVer.parse(latestFw.subSequence(firstNumberIndex, latestFw.length).toString())

            //TODO remove this "return true" for production
            return true

            if (sensorFwParsed.compareTo(latestFwParsed) == -1) {
                return true
            } else {
                _stage.value = DfuUpdateStage.ALREADY_LATEST_VERSION
                return false
            }
        }
    }

    fun getSensorFirmwareVersion() {
        _stage.value = DfuUpdateStage.CHECKING_CURRENT_FW_VERSION

        if (getFwJob != null && getFwJob?.isActive == true) {
            Timber.d("Already in sync mode")
            return
        }

        getFwJob = viewModelScope.launch {
            _sensorFwVersion.value = sensorFwVersionInteractor.getSensorFirmwareVersion(sensorId)
        }
    }

    fun getLatestFw() {
        viewModelScope.launch {
            val latestFw = latestFwInteractor.funGetLatestFwVersion()
            latestFwinfo = latestFw
            _latestFwVersion.value = latestFw?.tag_name
        }
    }

    fun startUpdateProcess(filesDir: File) {
        _stage.value = DfuUpdateStage.DOWNLOADING_FW

        val url = latestFwinfo?.assets?.first()?.browser_download_url
        val name = latestFwinfo?.assets?.first()?.name
        val file = File(filesDir, name)
        Timber.d("startUpdateProcess ${file.absolutePath} exists ${file.exists()}")
        if (file.exists()) {
            _stage.value = DfuUpdateStage.READY_FOR_UPDATE
            return
        }

        if (url != null) {
            viewModelScope.launch {
                latestFwInteractor.downloadFw(url, file.absolutePath) {
                    if (it is DownloadFileStatus.Progress) {
                        _downloadFwProgress.value = it.percent
                    } else if (it is DownloadFileStatus.Finished) {
                        _stage.value = DfuUpdateStage.READY_FOR_UPDATE
                    }
                }
            }
        }
    }
}

enum class DfuUpdateStage{
    CHECKING_CURRENT_FW_VERSION,
    ALREADY_LATEST_VERSION,
    DOWNLOADING_FW,
    READY_FOR_UPDATE,
    UPDATING_FW,
    UPDATE_FINISHED,
    ERROR
}