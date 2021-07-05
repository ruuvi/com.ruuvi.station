package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ShareSensorViewModel (
    val tagId: String,
    val ruuviNetworkInteractor: RuuviNetworkInteractor
) : ViewModel() {

    private val emails = MutableLiveData<List<String>>()
    val emailsObserve: LiveData<List<String>> = emails

    private val operationStatus = MutableLiveData<String> ("")
    val operationStatusObserve: LiveData<String> = operationStatus

    private val canShare = MutableLiveData<Boolean> (true)
    val canShareObserve: LiveData<Boolean> = canShare

    private val handler = CoroutineExceptionHandler { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = exception.message
        }
    }

    init {
        getSensorSharedEmails()
    }

    fun getSensorSharedEmails() {
        ruuviNetworkInteractor.getSharedInfo(tagId, handler) { response ->
            Timber.d("getSensorSharedEmails ${response.toString()}")
            emails.value = response?.sharedTo ?: listOf()
            canShare.value = response?.canShare ?: false
        }
    }

    fun shareTag(email: String) {
        ruuviNetworkInteractor.shareSensor(email, tagId, handler) { response ->
            if (response?.isError() == true) {
                operationStatus.value = response.error
            }
            getSensorSharedEmails()
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun unshareTag(email: String) {
        ruuviNetworkInteractor.unshareSensor(email, tagId, handler) { response ->
            if (response?.isError() == true) {
                operationStatus.value = response.error
            }
            getSensorSharedEmails()
        }
    }
}