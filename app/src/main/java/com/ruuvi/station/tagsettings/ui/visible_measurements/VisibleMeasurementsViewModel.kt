package com.ruuvi.station.tagsettings.ui.visible_measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ruuvi.station.dashboard.ui.swap
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class VisibleMeasurementsViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
    private val unitsConverter: UnitsConverter
    ): ViewModel() {

    private val _sensorState = MutableStateFlow<RuuviTag>(
        requireNotNull(
            interactor.getFavouriteSensorById(sensorId)
        )
    )
    val sensorState: StateFlow<RuuviTag> = _sensorState

    val useDefaultOrder: StateFlow<Boolean> =
        _sensorState
            .mapNotNull { it.defaultDisplayOrder }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(1000),
                true
            )

    val selected: StateFlow<List<ListOption>> =
        _sensorState
            .mapNotNull {
                it.displayOrder.mapNotNull { unitType ->
                    ListOption(
                    id = unitType.getCode(),
                    title = unitsConverter.getTitleForUnitType(unitType)
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(1000),
                listOf()
            )

    val possibleOptions: StateFlow<List<ListOption>> =
        _sensorState
            .mapNotNull {
                it.possibleDisplayOptions.mapNotNull { unitType ->
                    ListOption(
                        id = unitType.getCode(),
                        title = unitsConverter.getTitleForUnitType(unitType)
                    )
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(1000),
                listOf()
            )


    fun onAction(action: VisibleMeasurementsActions) {
        when (action) {
            is VisibleMeasurementsActions.ChangeUseDefault ->
                changeUseDefault(action.enabled)
            is VisibleMeasurementsActions.AddToDisplayOrder ->
                addToDisplayOrder(action.unitCode)
            is VisibleMeasurementsActions.RemoveFromDisplayOrder ->
                removeFromDisplayOrder(action.unitCode)
            is VisibleMeasurementsActions.SwapDisplayOrderItems ->
                swapDisplayOrderItems(action.from, action.to)
        }
    }

    fun addToDisplayOrder(unitCode: String) {
        val displayOrder = selected.value
            .mapNotNull { it.id }
            .toMutableList()
        displayOrder.add(unitCode)
        interactor.newDisplayOrder(sensorId, Gson().toJson(displayOrder))
        _sensorState.update { requireNotNull(interactor.getFavouriteSensorById(sensorId)) }
    }

    fun removeFromDisplayOrder(unitCode: String) {
        val displayOrder = selected.value
            .mapNotNull { it.id }
            .toMutableList()
        if (displayOrder.size > 1) {
            displayOrder.remove(unitCode)
            interactor.newDisplayOrder(sensorId, Gson().toJson(displayOrder))
            _sensorState.update { requireNotNull(interactor.getFavouriteSensorById(sensorId)) }
        }
    }

    fun swapDisplayOrderItems(from: Int, to: Int) {
        val displayOrder = selected.value
            .mapNotNull { it.id }
            .toMutableList()
        val swapped = displayOrder.swap(from, to)
        interactor.newDisplayOrder(sensorId, Gson().toJson(swapped))
        _sensorState.update { requireNotNull(interactor.getFavouriteSensorById(sensorId)) }
    }

    private fun changeUseDefault(useDefault: Boolean) {
        interactor.setUseDefaultSensorsOrder(sensorId, useDefault)
        _sensorState.update { requireNotNull(interactor.getFavouriteSensorById(sensorId)) }
    }
}