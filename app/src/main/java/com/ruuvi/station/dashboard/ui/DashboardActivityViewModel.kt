package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Timer
import kotlin.concurrent.scheduleAtFixedRate

@ExperimentalCoroutinesApi
class DashboardActivityViewModel(
    private val bluetoothInteractor: BluetoothInteractor,
    private val tagInteractor: TagInteractor
) : ViewModel() {

    private val tags = MutableStateFlow<List<RuuviTag>>(arrayListOf())
    val tagsFlow: StateFlow<List<RuuviTag>> = tags

    private val flowTimer = Timer("DashboardActivityViewModelTimer", false)
    private val ioScope = CoroutineScope(Dispatchers.IO)

    init {
        getTagsFlow()
    }

    fun startForegroundScanning() {
        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()
    }

    private fun getTagsFlow() {
        ioScope.launch {
            flowTimer.scheduleAtFixedRate(0, 500) {
                tagInteractor.getTags()
                    .let {
                        tags.value = it
                    }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ioScope.cancel()
    }
}