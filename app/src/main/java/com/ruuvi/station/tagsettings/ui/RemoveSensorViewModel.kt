package com.ruuvi.station.tagsettings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber

class RemoveSensorViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
    private val networkInteractor: RuuviNetworkInteractor,

    ): ViewModel() {

    private val _sensorState = MutableStateFlow<RuuviTag>(requireNotNull(interactor.getFavouriteSensorById(sensorId)))
    val sensorState: StateFlow<RuuviTag> = _sensorState

    val sensorOwnedByUser: Flow<Boolean> = sensorState.mapNotNull {
        it.owner?.isNotEmpty() == true && it.owner.equals(networkInteractor.getEmail(), true)
    }

    private var _removeWithCloudData = MutableStateFlow(false)
    val removeWithCloudData: StateFlow<Boolean> = _removeWithCloudData

    fun setRemoveWithCloudData(value: Boolean) {
        _removeWithCloudData.value = value
    }

    fun removeSensor() {
        Timber.d("removeSensor")
        interactor.deleteTagsAndRelatives(sensorId, _removeWithCloudData.value)
    }
}