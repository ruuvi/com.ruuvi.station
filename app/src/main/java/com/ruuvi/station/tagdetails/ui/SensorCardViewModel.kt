package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class SensorCardViewModel(
    private val arguments: SensorCardViewModelArguments,
    private val tagInteractor: TagInteractor,
): ViewModel() {

    val sensorsFlow: Flow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private val _selectedIndex = MutableStateFlow<Int?>(null)
    val selectedIndex: StateFlow<Int?> = _selectedIndex

    init {
        if (arguments.sensorId != null) {
            val sensors = tagInteractor.getTags()
            _selectedIndex.value = sensors.indexOfFirst { it.id == arguments.sensorId }
        }
    }

}

data class SensorCardViewModelArguments(
    val sensorId: String? = null,
    val showChart: Boolean = false
)