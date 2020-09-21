package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class DashboardActivityViewModel(
    private val bluetoothInteractor: BluetoothInteractor,
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter
) : ViewModel() {

    private val tags = MutableStateFlow<List<RuuviTag>>(arrayListOf())
    val tagsFlow: StateFlow<List<RuuviTag>> = tags

    private val flowTimer = Timer("DashboardActivityViewModelTimer", false)

    init {
        getTagsFlow()
    }

    fun startForegroundScanning() {
        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()
    }

    private fun getTagsFlow() {
        viewModelScope.launch {
            flowTimer.scheduleAtFixedRate(0, 500) {
                tags.value = tagInteractor.getTags()
            }
        }
    }
}