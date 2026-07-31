package com.ruuvi.station.settings.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import com.ruuvi.station.settings.domain.ResolutionSettingsTarget
import com.ruuvi.station.units.model.Accuracy
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import com.ruuvi.station.units.model.UnitType.PressureUnit
import com.ruuvi.station.units.model.UnitType.TemperatureUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlobalUnitsAndResolutionViewModel(
    private val appSettingsInteractor: AppSettingsInteractor
) : ViewModel() {

    val temperatureUnit: LiveData<TemperatureUnit> = appSettingsInteractor.getTemperatureUnitLiveData()

    val humidityUnit: LiveData<HumidityUnit> = appSettingsInteractor.getHumidityUnitLiveData()

    val pressureUnit: LiveData<PressureUnit> = appSettingsInteractor.getPressureUnitLiveData()

    private val _accuracyValues = MediatorLiveData<Map<ResolutionSettingsTarget, Accuracy>>().apply {
        value = currentAccuracyValues()
        ResolutionSettingsTarget.values().forEach { target ->
            addSource(appSettingsInteractor.getAccuracyLiveData(target)) {
                value = currentAccuracyValues()
            }
        }
    }
    val accuracyValues: LiveData<Map<ResolutionSettingsTarget, Accuracy>> = _accuracyValues

    private val _resolutionTargets = MutableLiveData<List<ResolutionSettingsTarget>>()
    val resolutionTargets: LiveData<List<ResolutionSettingsTarget>> = _resolutionTargets

    fun refresh() {
        _accuracyValues.value = currentAccuracyValues()
    }

    fun refreshResolutionTargets() {
        viewModelScope.launch(Dispatchers.IO) {
            _resolutionTargets.postValue(appSettingsInteractor.getResolutionTargets())
        }
    }

    fun getAllTemperatureUnits(): List<TemperatureUnit> =
        appSettingsInteractor.getAllTemperatureUnits()

    fun getAllHumidityUnits(): List<HumidityUnit> =
        appSettingsInteractor.getAllHumidityUnits()
            .filter { it != HumidityUnit.DewPoint }

    fun getAllPressureUnits(): List<PressureUnit> =
        appSettingsInteractor.getAllPressureUnits()

    fun getAccuracyList(): Array<Accuracy> =
        appSettingsInteractor.getAccuracyList()

    fun setTemperatureUnit(unit: TemperatureUnit) {
        appSettingsInteractor.setTemperatureUnit(unit)
        refresh()
    }

    fun setHumidityUnit(unit: HumidityUnit) {
        appSettingsInteractor.setHumidityUnit(unit)
        refresh()
    }

    fun setPressureUnit(unit: PressureUnit) {
        appSettingsInteractor.setPressureUnit(unit)
        refresh()
    }

    fun setAccuracy(target: ResolutionSettingsTarget, accuracy: Accuracy) {
        appSettingsInteractor.setAccuracy(target, accuracy)
        refresh()
    }

    private fun currentAccuracyValues(): Map<ResolutionSettingsTarget, Accuracy> =
        ResolutionSettingsTarget.values().associateWith { appSettingsInteractor.getAccuracy(it) }
}
