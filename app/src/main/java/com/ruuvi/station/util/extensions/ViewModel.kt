package com.ruuvi.station.util.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.network.domain.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date

const val DELAY_FOR_IN_PROGRESS = 500
fun ViewModel.processStatus(
    status: Flow<OperationStatus>,
    uiEventFlow: MutableSharedFlow<UiEvent>
) {
    fun getDelay(inProgress: Long?): Long? {
        val delayTimeout = inProgress?.let { DELAY_FOR_IN_PROGRESS - (Date().time - it) }
        return if (delayTimeout != null && delayTimeout > 0 && delayTimeout < DELAY_FOR_IN_PROGRESS) {
            delayTimeout
        } else {
            null
        }
    }

    viewModelScope.launch {
        var inProgress: Long? = null
        status.collect { status ->
            Timber.d("setName status $status")
            when (status) {
                OperationStatus.Success -> {
                    (Dispatchers.Main) {
                        getDelay(inProgress)?.let {
                            delay(it)
                        }
                        uiEventFlow.emit(
                            UiEvent.ShowSnackbar(
                                com.ruuvi.station.app.ui.UiText.StringResource(com.ruuvi.station.R.string.saving_success),
                                androidx.compose.material.SnackbarDuration.Short
                            )
                        )
                    }
                }

                is OperationStatus.Fail -> {
                    (Dispatchers.Main) {
                        getDelay(inProgress)?.let {
                            delay(it)
                        }
                        uiEventFlow.emit(
                            UiEvent.ShowSnackbar(
                                com.ruuvi.station.app.ui.UiText.StringResource(com.ruuvi.station.R.string.saving_fail),
                                androidx.compose.material.SnackbarDuration.Short
                            )
                        )
                    }
                }

                OperationStatus.InProgress -> {
                    inProgress = Date().time
                    Timber.d("delayTimeout inProgress set $inProgress")

                    (Dispatchers.Main) {
                        uiEventFlow.emit(
                            UiEvent.ShowSnackbar(
                                com.ruuvi.station.app.ui.UiText.StringResource(com.ruuvi.station.R.string.saving_to_cloud),
                                androidx.compose.material.SnackbarDuration.Indefinite
                            )
                        )
                    }
                }

                OperationStatus.Skipped -> {}
            }
        }
    }
}