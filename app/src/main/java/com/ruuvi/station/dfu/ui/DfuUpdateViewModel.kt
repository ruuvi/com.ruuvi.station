package com.ruuvi.station.dfu.ui

import androidx.lifecycle.*
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.AirFirmwareInteractor
import com.ruuvi.station.bluetooth.domain.*
import com.ruuvi.station.bluetooth.model.SensorFirmwareResult
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.isLowBattery
import com.ruuvi.station.dfu.data.DownloadFileStatus
import com.ruuvi.station.dfu.data.LatestReleaseResponse
import com.ruuvi.station.dfu.data.ReleaseAssets
import com.ruuvi.station.dfu.domain.LatestFwInteractor
import com.ruuvi.station.tag.domain.isAir
import com.ruuvi.station.util.MacAddressUtils.Companion.incrementMacAddress
import com.ruuvi.station.util.extensions.diffGreaterThan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.swiftzer.semver.SemVer
import timber.log.Timber
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*

class DfuUpdateViewModel(
    val sensorId: String,
    private val sensorInfoInteractor: SensorInfoInteractor,
    private val latestFwInteractor: LatestFwInteractor,
    private val bluetoothDevicesInteractor: BluetoothDevicesInteractor,
    private val dfuInteractor: DfuInteractor,
    private val tagRepository: TagRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val airFirmwareInteractor: AirFirmwareInteractor
): ViewModel() {

    private val _stage = MutableLiveData(DfuUpdateStage.CHECKING_CURRENT_FW_VERSION)
    val stage: LiveData<DfuUpdateStage> = _stage

    private val _sensorFwVersion = MutableLiveData<SensorFirmwareResult>(null)
    val sensorFwVersion: LiveData<SensorFirmwareResult> = _sensorFwVersion

    private val _latestFwVersion = MutableLiveData<String>(null)
    val latestFwVersion: LiveData<String> = _latestFwVersion

    private val _downloadFwProgress = MutableLiveData<Int>(0)
    val downloadFwProgress: LiveData<Int> = _downloadFwProgress

    private val _updateFwProgress = MutableLiveData<Int>(0)
    val updateFwProgress: LiveData<Int> = _updateFwProgress

    val canStartUpdate = MediatorLiveData<Boolean>()

    private val _deviceDiscovered = MutableLiveData<Boolean>(false)
    val deviceDiscovered: LiveData<Boolean> = _deviceDiscovered

    private val _error = MutableLiveData<String>(null)
    val error: LiveData<String> = _error

    private val _errorCode = MutableLiveData<Int>(null)
    val errorCode: LiveData<Int> = _errorCode

    private val _lowBattery = MutableLiveData<Boolean>(isLowBattery())
    val lowBattery: LiveData<Boolean> = _lowBattery

    private var latestFwinfo: LatestReleaseResponse? = null

    private var fwFile: File? = null
    var permissionsGranted: Boolean = false

    @Volatile
    private var getFwJob: Job? = null

    init {
        Timber.d("Init viewmodel for $sensorId ${sensorFwVersion.value}")

        val ruuviTag = tagRepository.getFavoriteSensorById(sensorId)

        if (ruuviTag?.isAir() == true) {
            initAirInteractor()
            _stage.value = DfuUpdateStage.AIR_UPDATE
        } else {
            _stage.value = DfuUpdateStage.CHECKING_CURRENT_FW_VERSION
            getLatestFw()
        }

        canStartUpdate.addSource(_sensorFwVersion) { canStartUpdate.value = updateAllowed() }
        canStartUpdate.addSource(_latestFwVersion) { canStartUpdate.value = updateAllowed() }
        canStartUpdate.addSource(_stage) { canStartUpdate.value = updateAllowed() }
    }

    fun initAirInteractor() {
        airFirmwareInteractor.connect(sensorId)
    }

    fun permissionsChecked(permissionsGranted: Boolean) {
        Timber.d("permissionsChecked $permissionsGranted")
        this.permissionsGranted = permissionsGranted
        if (permissionsGranted) {
            getSensorFirmwareVersion()
        }
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
            try {
                val sensorFwFirstNumberIndex = sensorFw.fw.indexOfFirst { it.isDigit() }
                val sensorFwParsed = SemVer.parse(sensorFw.fw.subSequence(sensorFwFirstNumberIndex, sensorFw.fw.length).toString())
                val latestFwFirstNumberIndex = latestFw.indexOfFirst { it.isDigit() }
                val latestFwParsed = SemVer.parse(latestFw.subSequence(latestFwFirstNumberIndex, latestFw.length).toString())

                if (sensorFwParsed.compareTo(latestFwParsed) < 0) {
                    return true
                } else {
                    _stage.value = DfuUpdateStage.ALREADY_LATEST_VERSION
                    return false
                }
            } catch (e: IllegalArgumentException) {
                return true
            }
        }
    }

    fun getSensorFirmwareVersion() {
        if (getFwJob != null && getFwJob?.isActive == true) {
            Timber.d("Already in sync mode")
            return
        }

        getFwJob = viewModelScope.launch {
            Timber.d("getSensorFirmwareVersion")
            val firmware = sensorInfoInteractor.getSensorFirmwareVersion(sensorId)
            _sensorFwVersion.value = firmware
            if (firmware.isSuccess && firmware.fw.isNotEmpty()) {
                sensorSettingsRepository.setSensorFirmware(sensorId, firmware.fw)
            }
        }
    }

    fun getLatestFw() {
        viewModelScope.launch {
            try {
                val latestFw = latestFwInteractor.funGetLatestFwVersion()
                latestFwinfo = latestFw
                _latestFwVersion.value = latestFw?.tag_name
            } catch (e: Exception) {
                Timber.e(e, "getLatestFw error")
                error(R.string.dfu_no_internet)
            }
        }
    }

    fun startDownloadProcess(filesDir: File) {
        _stage.value = DfuUpdateStage.DOWNLOADING_FW
        val asset = selectAsset(latestFwinfo?.assets ?: listOf())

        Timber.d("selected asset = $asset)")
        asset?.let {
            val url = asset.browser_download_url
            val name = asset.name
            fwFile = File(filesDir, name)
            Timber.d("startUpdateProcess ${fwFile?.absolutePath} exists ${fwFile?.exists()}")
            if (fwFile?.exists() == true) {
                searchForDevice()
                return
            }

            viewModelScope.launch {
                try {
                    fwFile?.let { file ->
                        latestFwInteractor.downloadFw(url, file.absolutePath) {
                            if (it is DownloadFileStatus.Progress) {
                                _downloadFwProgress.value = it.percent
                            } else if (it is DownloadFileStatus.Finished) {
                                searchForDevice()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "startDownloadProcess error")
                    error(R.string.dfu_no_internet)
                }
            }
        }
    }

    private fun selectAsset(assets: List<ReleaseAssets>): ReleaseAssets? {
        val sensorFw = _sensorFwVersion.value

        val pattern =
            if (sensorFw?.isSuccess != true) {
                PATTERN_2_TO_3
            } else {
                try {
                    val sensorFwFirstNumberIndex = sensorFw.fw.indexOfFirst { it.isDigit() }
                    if (sensorFwFirstNumberIndex == -1) throw IllegalArgumentException()
                    val sensorFwParsed = SemVer.parse(
                        sensorFw.fw.subSequence(
                            sensorFwFirstNumberIndex,
                            sensorFw.fw.length
                        ).toString()
                    )

                    if (sensorFwParsed.major < 3) {
                        PATTERN_2_TO_3
                    } else {
                        PATTERN_3x
                    }
                } catch (e: IllegalArgumentException) {
                    PATTERN_2_TO_3
                }
            }

        val regex = Regex(pattern)
        return assets.firstOrNull { regex.matches(it.name) }
    }

    private fun searchForDevice() {
        _stage.value = DfuUpdateStage.READY_FOR_UPDATE

        val sensorUpdateMac = incrementMacAddress(sensorId)
        viewModelScope.launch {
            var deviceDiscoveredDate: Date? = null
            while (_stage.value == DfuUpdateStage.READY_FOR_UPDATE) {
                Timber.d("discovering Devices")
                bluetoothDevicesInteractor.cancelDiscovery()
                delay(1000)
                bluetoothDevicesInteractor.discoverDevices() {
                    Timber.d("discoverDevices $it")
                    if (it.mac == sensorUpdateMac) {
                        _deviceDiscovered.postValue(true)
                        deviceDiscoveredDate = Date()
                        bluetoothDevicesInteractor.cancelDiscovery()
                    }
                }
                delay(9000)
                if (deviceDiscoveredDate != null && deviceDiscoveredDate?.diffGreaterThan(10000) == true) {
                    _deviceDiscovered.postValue(false)
                }
            }
        }
    }

    fun startUpdateProcess() {
        bluetoothDevicesInteractor.cancelDiscovery()
        _stage.value = DfuUpdateStage.UPDATING_FW
        _updateFwProgress.value = 0

        dfuInteractor.startDfuUpdate(incrementMacAddress(sensorId), requireNotNull(fwFile)) {
            when (it) {
                is DfuUpdateStatus.Progress -> {
                    _updateFwProgress.value = it.percent
                }
                is DfuUpdateStatus.Error -> {
                    error(it.message)
                }
                is DfuUpdateStatus.Finished -> {
                    updateFinished()
                }
            }
        }
    }

    private fun isLowBattery(): Boolean {
        val tagInfo = tagRepository.getTagById(sensorId)
        return tagInfo?.isLowBattery() ?: false
    }

    private fun error(message: String?) {
        _error.value = message
        _stage.value = DfuUpdateStage.ERROR
    }

    private fun error(messageId: Int) {
        _errorCode.value = messageId
        _stage.value = DfuUpdateStage.ERROR
    }

    private fun updateFinished() {
        _stage.value = DfuUpdateStage.UPDATE_FINISHED
        sensorSettingsRepository.setSensorFirmware(sensorId, latestFwVersion.value)
    }

    override fun onCleared() {
        super.onCleared()
        if (permissionsGranted) {
            bluetoothDevicesInteractor.cancelDiscovery()
        }
    }

    fun upload(fileName: String, readBytesFromUri: ByteArray) {
        airFirmwareInteractor.upload(fileName, readBytesFromUri, { current, total ->
            viewModelScope.launch {
                _updateFwProgress.value = (current.toDouble() / total * 100).toInt()
            }
        },
            {
                viewModelScope.launch {
                    updateFinished()
                }
            })
    }

    companion object {
        const val PATTERN_2_TO_3 = "ruuvitag.*default.*_sdk12\\.3_to_15\\.3_dfu\\.zip"
        const val PATTERN_3x = "ruuvitag.*default.*dfu_app\\.zip"
    }
}

enum class DfuUpdateStage{
    CHECKING_CURRENT_FW_VERSION,
    ALREADY_LATEST_VERSION,
    DOWNLOADING_FW,
    READY_FOR_UPDATE,
    UPDATING_FW,
    UPDATE_FINISHED,
    ERROR,
    AIR_UPDATE
}