package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AppSettingsGraphViewModel(
    private val interactor: AppSettingsInteractor
) : ViewModel() {
    private val pointInterval = Channel<Int>(1)
    private val viewPeriod = Channel<Int>(1)

    private val showAllPoints = MutableStateFlow(interactor.isShowAllGraphPoint())
    val showAllPointsFlow: StateFlow<Boolean> = showAllPoints

    init {
        viewModelScope.launch {
            pointInterval.send(interactor.getGraphPointInterval())
            viewPeriod.send(interactor.getGraphViewPeriod())
        }
    }

    fun observePointInterval(): Flow<Int> = pointInterval.receiveAsFlow()

    fun observeViewPeriod(): Flow<Int> = viewPeriod.receiveAsFlow()

    fun setPointInterval(newInterval: Int) =
        interactor.setGraphPointInterval(newInterval)

    fun setViewPeriod(newPeriod: Int) =
        interactor.setGraphViewPeriod(newPeriod)

    fun setShowAllPoints(isChecked: Boolean) {
        interactor.setIsShowAllGraphPoint(isChecked)
        showAllPoints.value = isChecked
    }
}