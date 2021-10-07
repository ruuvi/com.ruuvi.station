package com.ruuvi.station.dfu.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.bluetooth.domain.SensorVersionInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.generic.instance
import timber.log.Timber

class DfuUpdateViewModel(
    val sensorId: String,
    val sensorVersionInteractor: SensorVersionInteractor
    ): ViewModel() {

    fun startUpdate() {

    }

    private val _stage = MutableLiveData(DfuUpdateStage.CHECKING_CURRENT_FW_VERSION)
    val stage: LiveData<DfuUpdateStage> = _stage

    private val _sensorFwVersion = MutableLiveData<String>(null)
    val sensorFwVersion: LiveData<String> = _sensorFwVersion

    @Volatile
    private var getFwJob: Job? = null

    init {
        Timber.d("Init viewmodel for $sensorId ${sensorFwVersion.value}")
        getSensorFirmwareVersion()
    }

    fun getSensorFirmwareVersion() {
        _stage.value = DfuUpdateStage.CHECKING_CURRENT_FW_VERSION

        if (getFwJob != null && getFwJob?.isActive == true) {
            Timber.d("Already in sync mode")
            return
        }

        getFwJob = viewModelScope.launch {
            val getFwResult = sensorVersionInteractor.getSensorFirmwareVersion(sensorId)
            if (getFwResult.isSuccess) {
                _sensorFwVersion.value = getFwResult.fw
                _stage.value = DfuUpdateStage.READY_FOR_UPDATE
            } else {
                _sensorFwVersion.value = getFwResult.error
                //_stage.value = DfuUpdateStage.ERROR
            }
        }
    }
}

enum class DfuUpdateStage{
    CHECKING_CURRENT_FW_VERSION,
    ALREADY_LATEST_VERSION,
    READY_FOR_UPDATE,
    DOWNLOADING_FW,
    UPDATING_FW,
    UPDATE_FINISHED,
    ERROR
}