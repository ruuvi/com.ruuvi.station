package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.ui.model.ShareOperationStatus
import com.ruuvi.station.network.ui.model.ShareOperationType
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ShareSensorViewModel (
    val sensorId: String,
    val ruuviNetworkInteractor: RuuviNetworkInteractor
) : ViewModel() {

    private val emails = MutableLiveData<List<String>>()
    val emailsObserve: LiveData<List<String>> = emails

    private val operationStatus = MutableLiveData<ShareOperationStatus?> (null)
    val operationStatusObserve: LiveData<ShareOperationStatus?> = operationStatus

    private val canShare = MutableLiveData<Boolean> (true)
    val canShareObserve: LiveData<Boolean> = canShare

    private val handler = CoroutineExceptionHandler { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_ERROR, exception.message ?: "")
        }
    }

    init {
        getSensorSharedEmails()
    }

    fun getSensorSharedEmails() {
        ruuviNetworkInteractor.getSharedInfo(sensorId, handler) { response ->
            Timber.d("getSensorSharedEmails ${response.toString()}")
            emails.value = response?.sharedTo ?: listOf()
            canShare.value = response?.canShare ?: false
        }
    }

    fun shareTag(email: String) {
        ruuviNetworkInteractor.shareSensor(email, sensorId, handler) { response ->
            if (response?.isError() == true) {
                operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_ERROR, response.error)
            } else {
                operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_SUCCESS, "")
            }
            getSensorSharedEmails()
        }
    }

    fun statusProcessed() { operationStatus.value = null }

    fun unshareTag(email: String) {
        ruuviNetworkInteractor.unshareSensor(email, sensorId, handler) { response ->
            if (response?.isError() == true) {
                operationStatus.value = ShareOperationStatus(ShareOperationType.SHARING_ERROR, response.error)
            }
            getSensorSharedEmails()
        }
    }
}