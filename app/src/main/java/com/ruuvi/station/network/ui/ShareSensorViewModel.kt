package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.SensorShareListRepository
import com.ruuvi.station.network.data.request.SensorDenseRequest
import com.ruuvi.station.network.data.response.UserVerifyResponseBody
import com.ruuvi.station.network.domain.NetworkShareListInteractor
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

class ShareSensorViewModel (
    val sensorId: String,
    val ruuviNetworkInteractor: RuuviNetworkInteractor,
    val networkShareListInteractor: NetworkShareListInteractor,
    val sensorShareListRepository: SensorShareListRepository,
    val sensorSettingsRepository: SensorSettingsRepository,
    val preferencesRepository: PreferencesRepository,
    val networkTokenRepository: NetworkTokenRepository
) : ViewModel() {

    private val emails = MutableLiveData<List<String>>()
    val emailsObserve: LiveData<List<String>> = emails

    private val canShare = MutableLiveData<Boolean> (false)
    val canShareObserve: LiveData<Boolean> = canShare

    private val _uiEvent = MutableSharedFlow<UiEvent> ()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    val useWebShare: LiveData<Boolean> = MutableLiveData<Boolean> (preferencesRepository.getUseWebShare())

    private val handler = CoroutineExceptionHandler { _, exception ->
        CoroutineScope(Dispatchers.Main).launch {
            _uiEvent.emit(UiEvent.ShowSnackbar(UiText.DynamicString(exception.message ?: "")))
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

    fun getWebViewToken(): String? {
        val tokenInfo = networkTokenRepository.getTokenInfo()
        if (tokenInfo != null) {
            val userRegisterResponse = UserVerifyResponseBody(tokenInfo.email, tokenInfo.token, false)
            return Gson().toJson(userRegisterResponse)
        } else {
            return null
        }

    }

    fun getUrl(): String {
        return "https://station.ruuvi.com/shares?sensor=$sensorId&minimalMode=true"
    }
    private fun setEmailsFromRepository() {
        emails.value = sensorShareListRepository.getShareListForSensor(sensorId).map { it.userEmail }
    }

    fun shareTag(email: String) {
        ruuviNetworkInteractor.shareSensor(email, sensorId, handler) { response ->
            if (response?.isError() == true) {
                CoroutineScope(Dispatchers.Main).launch{
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.DynamicString(response.error)))
                }
            } else {
                if (response?.data?.invited == true) {
                    CoroutineScope(Dispatchers.Main).launch{
                        _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.share_pending_message)))
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch{
                        _uiEvent.emit(UiEvent.ShowSnackbar(UiText.StringResource(R.string.successfully_shared)))
                    }
                    sensorShareListRepository.insertToShareList(sensorId, email)
                    setEmailsFromRepository()
                }
            }
        }
    }

    fun unshareTag(email: String) {
        ruuviNetworkInteractor.unshareSensor(email, sensorId, handler) { response ->
            if (response?.isError() == true) {
                CoroutineScope(Dispatchers.Main).launch{
                    _uiEvent.emit(UiEvent.ShowSnackbar(UiText.DynamicString(response.error)))
                }
            } else {
                sensorShareListRepository.deleteFromShareList(sensorId, email)
                setEmailsFromRepository()
            }
        }
    }
}