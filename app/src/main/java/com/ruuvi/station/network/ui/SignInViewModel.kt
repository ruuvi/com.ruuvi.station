package com.ruuvi.station.network.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.app.ui.UiText
import com.ruuvi.station.network.data.NetworkSyncEvent
import com.ruuvi.station.network.data.request.UserRegisterRequest
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class SignInViewModel(
    val networkInteractor: RuuviNetworkInteractor,
    val networkSignInInteractor: NetworkSignInInteractor,
    val networkDataSyncInteractor: NetworkDataSyncInteractor
): ViewModel() {

    private val _uiEvent = MutableSharedFlow<UiEvent> ()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    private val _tokenProcessed = MutableSharedFlow<Boolean> ()
    val tokenProcessed: SharedFlow<Boolean> = _tokenProcessed

    private val _email = MutableStateFlow<String> ("")
    val email: StateFlow<String> = _email

    val ioScope = CoroutineScope(Dispatchers.IO)

    fun submitEmail(email: String) {
        if (email.isNullOrEmpty()) {
            showError(UiText.StringResource(R.string.cloud_er_invalid_email_address))
            return
        }
        setProgress(true)

        _email.value = email

        networkInteractor.registerUser(UserRegisterRequest(email = email.trim())) {
            if (it == null) {
                //TODO LOCALIZE
                showError(UiText.DynamicString("Unknown error"))
            } else {
                if (it.error.isNullOrEmpty() == false) {
                    showError(UiText.DynamicString(it.error))
                } else {
                    goToPage(SignInRoutes.ENTER_CODE)
                }
            }
            setProgress(false)
        }
    }

    fun verifyCode(token: String) {
        setProgress(true)

        networkSignInInteractor.signIn(token) { response ->
            if (response.isNullOrEmpty()) {
                viewModelScope.launch {
                    networkDataSyncInteractor.syncEvents
                        .collect {
                            Timber.d("syncEvents collected $it")
                            if (it is NetworkSyncEvent.Success || it is NetworkSyncEvent.SensorsSynced) {
                                setProgress(false)
                                signInFinished()
                                this.cancel()
                            }
                        }
                }.invokeOnCompletion { Timber.d("ioScope collecting syncEvents Completed") }
                viewModelScope.launch {
                    val syncJob = networkDataSyncInteractor.syncNetworkData()
                    syncJob.join()
                    setProgress(false)
                    signInFinished()
                }.invokeOnCompletion { Timber.d("ioScope syncNetworkData Completed") }
            } else {
                showError(UiText.DynamicString(response))
                setProgress(false)
            }
            viewModelScope.launch {
                _tokenProcessed.emit(true)
            }
        }
    }

    fun requestToSkipSignIn() {
        goToPage(SignInRoutes.CLOUD_BENEFITS)
    }

    private fun showError(message: UiText) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.ShowSnackbar(message))
        }
    }

    private fun goToPage(destination: String, popBackStack: Boolean = false) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Navigate(destination, popBackStack))
        }
    }

    private fun setProgress(inProgress: Boolean, message: UiText? = null) {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Progress(inProgress, message))
        }
    }

    private fun signInFinished() {
        viewModelScope.launch {
            _uiEvent.emit(UiEvent.Finish)
        }
    }
}