package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.app.preferences.Preferences
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AppSettingsGraphViewModel(private val preferences: Preferences) : ViewModel() {
    private val pointInterval = Channel<Int>(1)
    private val viewPeriod = Channel<Int>(1)

    private val showAllPoints = MutableStateFlow<Boolean>(preferences.graphShowAllPoint)
    val showAllPointsFlow: StateFlow<Boolean> = showAllPoints

    init {
        viewModelScope.launch {
            pointInterval.send(preferences.graphPointInterval)
            viewPeriod.send(preferences.graphViewPeriod)
        }
    }

    fun observePointInterval(): Flow<Int> = pointInterval.receiveAsFlow()

    fun observeViewPeriod(): Flow<Int> = viewPeriod.receiveAsFlow()

    fun setPointInterval(newInterval: Int) {
        preferences.graphPointInterval = newInterval
    }

    fun setViewPeriod(newPeriod: Int) {
        preferences.graphViewPeriod = newPeriod
    }

    fun setShowAllPoints(checked: Boolean) {
        preferences.graphShowAllPoint = checked
        showAllPoints.value = checked
    }
}