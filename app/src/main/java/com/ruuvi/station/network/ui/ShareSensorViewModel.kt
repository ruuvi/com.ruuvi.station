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

    private val handler = CoroutineExceptionHandler() { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            operationStatus.value = exception.message
        }
    }

    init {
        getSensorSharedEmails()
    }

    fun getSensorSharedEmails() {
        ruuviNetworkInteractor.getShаredInfo(tagId, handler) { response ->
            Timber.d("getSensorSharedEmails ${response.toString()}")
            if (response?.isNotEmpty() == true) {
                emails.value = response.map { it.sharedTo }
            } else {
                emails.value = listOf<String>()
            }
        }
    }

    fun shareTag(email: String) {
        ruuviNetworkInteractor.shareSensor(email, tagId, handler) { response ->
            if (response?.result == "error") {
                operationStatus.value = response.error
            }
            getSensorSharedEmails()
        }
    }

    fun statusProcessed() { operationStatus.value = "" }

    fun unshareTag(email: String) {
        ruuviNetworkInteractor.unshareSensor(email, tagId, handler) { response ->
            if (response?.result == "error") {
                operationStatus.value = response.error
            }
            getSensorSharedEmails()
        }
    }
}