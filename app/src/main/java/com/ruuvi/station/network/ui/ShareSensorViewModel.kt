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

    private val sharingEnabled = MutableLiveData<Boolean> (false)
    val sharingEnabledObserve: LiveData<Boolean> = sharingEnabled

    private val unshareAllConfirm = MutableLiveData<Boolean> (false)
    val unshareAllConfirmObserve: LiveData<Boolean> = unshareAllConfirm

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
                sharingEnabled.value = true
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

    fun sharingEnabled(checked: Boolean) {
        sharingEnabled.value = checked
        val sharedCount = emails.value?.size ?: 0
        if (!checked && sharedCount > 0) unshareAllConfirm.value = true
    }

    fun unshareAll() {
        ruuviNetworkInteractor.unshareAll(tagId, handler) {
            getSensorSharedEmails()
        }
    }

    fun unshareAllConfirmDismiss() {
        unshareAllConfirm.value = false
    }
}