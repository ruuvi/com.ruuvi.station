package com.ruuvi.station.tagsettings.ui.visible_measurements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.ruuvi.station.alarm.domain.AlarmItemState
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.alarm.domain.AlarmsInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.ui.swap
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

class VisibleMeasurementsViewModel(
    val sensorId: String,
    private val interactor: TagSettingsInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val unitsConverter: UnitsConverter,
    private val alarmsInteractor: AlarmsInteractor
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
                    Timber.d("selected $unitType")
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

    val dashBoardType = preferencesRepository.getDashboardType()

    private val _effects = MutableSharedFlow<VisibleMeasurementsEffect>(extraBufferCapacity = 1)
    val effects = _effects.asSharedFlow()

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

            is VisibleMeasurementsActions.RemoveFromDisplayOrderAndDisableAlert -> {
                disableAlertAndRemove(action.unitCode)
            }

            is VisibleMeasurementsActions.ChangeUseDefaultAndDisableAlert -> TODO()
        }
    }

    private fun addToDisplayOrder(unitCode: String) {
        val displayOrder = selected.value
            .mapNotNull { it.id }
            .toMutableList()
        displayOrder.add(unitCode)
        interactor.newDisplayOrder(sensorId, Gson().toJson(displayOrder))
        _sensorState.update { requireNotNull(interactor.getFavouriteSensorById(sensorId)) }
    }

    private fun removeFromDisplayOrder(unitCode: String) {
        val displayOrder = selected.value
            .mapNotNull { it.id }
            .toMutableList()
        if (displayOrder.size > 1) {
            if (shouldConfirm(unitCode)) {
                viewModelScope.launch {
                    _effects.emit(VisibleMeasurementsEffect.AskRemovalConfirmation(unitCode))
                }
            } else {
                actualDelete(unitCode)
            }
        } else {
            viewModelScope.launch {
                _effects.emit(VisibleMeasurementsEffect.ForbiddenRemoveLast)
            }
        }
    }

    private fun disableAlertAndRemove(unitCode: String) {
        val unit = UnitType.getByCode(unitCode)
        val alarm = getAlertByUnit(unit)
        Timber.d("disableAlertAndRemove $unitCode $alarm")
        alarm?.let {
            it.isEnabled.value = false
            alarmsInteractor.saveAlarm(it)
            actualDelete(unitCode)
        }
    }

    private fun shouldConfirm(unitCode: String): Boolean {
        val unit = UnitType.getByCode(unitCode)
        val displayOrder = selected.value
            .mapNotNull { UnitType.getByCode(it.id) }
        val alarm = getAlertByUnit(unit)

        return when (unit) {
            is UnitType.TemperatureUnit -> {
                if (displayOrder.count { it is UnitType.TemperatureUnit } > 1) {
                    false
                } else {
                    alarm?.isEnabled?.value ?: false
                }
            }

            is UnitType.HumidityUnit -> {
                if (displayOrder.count { it is UnitType.HumidityUnit } > 1) {
                    false
                } else {
                    alarm?.isEnabled?.value ?: false
                }
            }

            is UnitType.PressureUnit -> {
                if (displayOrder.count { it is UnitType.PressureUnit } > 1) {
                    false
                } else {
                    alarm?.isEnabled?.value ?: false
                }
            }
            else -> {
                alarm?.isEnabled?.value ?: false
            }
        }
    }

    private fun actualDelete(unitCode: String) {
        val displayOrder = selected.value
            .mapNotNull { it.id }
            .toMutableList()
        if (displayOrder.size > 1) {
            displayOrder.remove(unitCode)
            interactor.newDisplayOrder(sensorId, Gson().toJson(displayOrder))
            _sensorState.update { requireNotNull(interactor.getFavouriteSensorById(sensorId)) }
        }
    }

    private fun swapDisplayOrderItems(from: Int, to: Int) {
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

    private fun getAlertByUnit(unitType: UnitType?): AlarmItemState? {
        val alarms = alarmsInteractor.getAlarmsForSensor(sensorId)

        val alarm = when (unitType) {
            is UnitType.TemperatureUnit -> alarms.firstOrNull { it.type == AlarmType.TEMPERATURE }
            is UnitType.HumidityUnit -> alarms.firstOrNull { it.type == AlarmType.HUMIDITY }
            is UnitType.PressureUnit -> alarms.firstOrNull { it.type == AlarmType.PRESSURE }
            UnitType.AirQuality.AqiIndex -> alarms.firstOrNull { it.type == AlarmType.AQI }
            UnitType.CO2.Ppm -> alarms.firstOrNull { it.type == AlarmType.CO2 }
            UnitType.Luminosity.Lux -> alarms.firstOrNull { it.type == AlarmType.LUMINOSITY }
            UnitType.MovementUnit.MovementsCount -> alarms.firstOrNull { it.type == AlarmType.MOVEMENT }
            UnitType.VOC.VocIndex -> alarms.firstOrNull { it.type == AlarmType.VOC }
            UnitType.NOX.NoxIndex -> alarms.firstOrNull { it.type == AlarmType.NOX }
            UnitType.PM.PM10 -> alarms.firstOrNull { it.type == AlarmType.PM10 }
            UnitType.PM.PM100 -> alarms.firstOrNull { it.type == AlarmType.PM100 }
            UnitType.PM.PM25 -> alarms.firstOrNull { it.type == AlarmType.PM25 }
            UnitType.PM.PM40 -> alarms.firstOrNull { it.type == AlarmType.PM40 }
            UnitType.SignalStrengthUnit.SignalDbm -> alarms.firstOrNull { it.type == AlarmType.RSSI }
            UnitType.SoundAvg.SoundDba -> alarms.firstOrNull { it.type == AlarmType.SOUND }
            else -> null
        }
        return alarm
    }

}