package com.ruuvi.station.util.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.app.ui.UiEvent
import com.ruuvi.station.network.domain.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import timber.log.Timber

fun ViewModel.processStatus(
    status: Flow<OperationStatus>,
    uiEventFlow: MutableSharedFlow<UiEvent>
) {
    viewModelScope.launch {
        status.collect { status ->
            Timber.d("setName status $status")
            when (status) {
                OperationStatus.Success -> {
                    (Dispatchers.Main) {
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
                        uiEventFlow.emit(
                            UiEvent.ShowSnackbar(
                                com.ruuvi.station.app.ui.UiText.StringResource(com.ruuvi.station.R.string.saving_fail),
                                androidx.compose.material.SnackbarDuration.Short
                            )
                        )
                    }
                }

                OperationStatus.InProgress -> {
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