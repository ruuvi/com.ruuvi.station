package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.SensorShareListRepository
import com.ruuvi.station.network.data.request.SensorDenseRequest
import com.ruuvi.station.network.domain.NetworkShareListInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.ui.model.ShareOperationStatus
import com.ruuvi.station.network.ui.model.ShareOperationType
import kotlinx.coroutines.*
import timber.log.Timber

class ShareSensorViewModel (
    val sensorId: String,
    val ruuviNetworkInteractor: RuuviNetworkInteractor,
    val networkShareListInteractor: NetworkShareListInteractor,
    val sensorShareListRepository: SensorShareListRepository,
    val sensorSettingsRepository: SensorSettingsRepository
) : ViewModel() {

    private val emails = MutableLiveData<List<String>>()
    val emailsObserve: LiveData<List<String>> = emails

    private val operationStatus = MutableLiveData<ShareOperationStatus?> (null)
    val operationStatusObserve: LiveData<ShareOperationStatus?> = operationStatus

    private val canShare = MutableLiveData<Boolean> (false)
    val canShareObserve: LiveData<Boolean> = canShare

    private val handler = CoroutineExceptionHandler { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_ERROR, exception.message ?: "")
        }
    }

    init {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        if (sensorSettings?.canShare == null) {
            val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
                Timber.e(throwable, "Failed to check sensor status")
            }
            (viewModelScope + coroutineExceptionHandler).launch {
                val denseData = ruuviNetworkInteractor.getSensorDenseLastData(
                    SensorDenseRequest(
                        sensor = sensorId,
                        sharedToOthers = true,
                        sharedToMe = true,
                        measurements = false,
                        alerts = false
                    )
                )
                if (denseData != null && denseData.isSuccess()) {
                    networkShareListInteractor.updateSharingInfo(denseData)
                    val sensorResponse = denseData.data?.sensors?.firstOrNull{it.sensor == sensorId}
                    canShare.value = sensorResponse?.canShare ?: false
                    setEmailsFromRepository()
                }
            }
        } else {
            canShare.value = sensorSettings.canShare ?: false
            setEmailsFromRepository()
        }
    }

    private fun setEmailsFromRepository() {
        emails.value = sensorShareListRepository.getShareListForSensor(sensorId).map { it.userEmail }
    }

    fun shareTag(email: String) {
        ruuviNetworkInteractor.shareSensor(email, sensorId, handler) { response ->
            if (response?.isError() == true) {
                operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_ERROR, response.error)
            } else {
                operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_SUCCESS, "")
                sensorShareListRepository.insertToShareList(sensorId, email)
                setEmailsFromRepository()
            }
        }
    }

    fun statusProcessed() { operationStatus.value = null }

    fun unshareTag(email: String) {
        ruuviNetworkInteractor.unshareSensor(email, sensorId, handler) { response ->
            if (response?.isError() == true) {
                operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_ERROR, response.error)
            } else {
                sensorShareListRepository.deleteFromShareList(sensorId, email)
                setEmailsFromRepository()
            }
        }
    }
}