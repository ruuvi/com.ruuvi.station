package com.ruuvi.station.widgets.complexWidget

import androidx.compose.runtime.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.ICloudWidgetViewModel
import timber.log.Timber

class ComplexWidgetConfigureViewModel(
    private val appWidgetId: Int,
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor
    ): ViewModel(), ICloudWidgetViewModel {

    private val _allSensors = MutableLiveData<List<RuuviTag>> (tagRepository.getFavoriteSensors())

    private val cloudSensors = Transformations.map(_allSensors) { allSensors ->
        allSensors.filter { it.networkLastSync != null }
    }

    val gotFilteredSensors = Transformations.map(_allSensors) { allSensors ->
        allSensors.any { it.networkLastSync == null }
    }

    private val _widgetItems = getSensorsForWidgetId(appWidgetId).toMutableStateList()
    val widgetItems: List<ComplexWidgetSensorItem> = _widgetItems

    private val _canBeSaved = MutableLiveData<Boolean> (false)
    override val canBeSaved: LiveData<Boolean> = _canBeSaved

    override val userLoggedIn: LiveData<Boolean> = MutableLiveData<Boolean> (networkInteractor.signedIn)

    override val userHasCloudSensors: LiveData<Boolean> = Transformations.map(cloudSensors) {
        it.isNotEmpty()
    }

    private val _setupComplete = MutableLiveData<Boolean> (false)
    val setupComplete: LiveData<Boolean> = _setupComplete

    override fun save() {
        _setupComplete.value = true
    }

    fun getSensorsForWidgetId(appWidgetId: Int): List<ComplexWidgetSensorItem> {
        return tagRepository.getFavoriteSensors().filter { it.networkLastSync != null }.map { cloudSensor ->
            ComplexWidgetSensorItem(cloudSensor.id, cloudSensor.displayName, mutableStateOf(false)).also {
                it.sensorTypes.addAll(ComplexWidgetSensorItem.defaultSet)
            }
        }
    }

    fun selectSensor(item: ComplexWidgetSensorItem, checked: Boolean) {
        Timber.d("selectSensor ${item.sensorId}")
        widgetItems.find { it.sensorId == item.sensorId }?.let {
            it.checked = mutableStateOf(checked)
        }
    }

    fun selectWidgetType(item: ComplexWidgetSensorItem, widgetType: WidgetType, checked: Boolean) {
        widgetItems.find { it.sensorId == item.sensorId }?.let {
            when (widgetType) {
                WidgetType.TEMPERATURE -> it.checkedTemperature = checked
                WidgetType.HUMIDITY -> it.checkedHumidity = checked
                WidgetType.PRESSURE -> it.checkedPressure = checked
                WidgetType.MOVEMENT -> it.checkedMovement = checked
                WidgetType.VOLTAGE -> it.checkedVoltage = checked
                WidgetType.SIGNAL_STRENGTH -> it.checkedSignalStrength = checked
                WidgetType.ACCELERATION_X -> it.checkedAccelerationX = checked
                WidgetType.ACCELERATION_Y -> it.checkedAccelerationY = checked
                WidgetType.ACCELERATION_Z -> it.checkedAccelerationZ = checked
            }
        }
    }
}

class ComplexWidgetSensorItem(
    val sensorId: String,
    var sensorName: String,
    selected: MutableState<Boolean>,
    sensorTypes: MutableSet<WidgetType> = mutableSetOf()
) {
    var checked by mutableStateOf(selected)
    var sensorTypes by mutableStateOf(sensorTypes)

    var checkedTemperature by mutableStateOf(false)
    var checkedHumidity by mutableStateOf(false)
    var checkedPressure by mutableStateOf(false)
    var checkedMovement by mutableStateOf(false)
    var checkedVoltage by mutableStateOf(false)
    var checkedSignalStrength by mutableStateOf(false)
    var checkedAccelerationX by mutableStateOf(false)
    var checkedAccelerationY by mutableStateOf(false)
    var checkedAccelerationZ by mutableStateOf(false)

    fun getStateForType(widgetType: WidgetType): Boolean {
        return when (widgetType) {
            WidgetType.TEMPERATURE -> checkedTemperature
            WidgetType.HUMIDITY -> checkedHumidity
            WidgetType.PRESSURE -> checkedPressure
            WidgetType.MOVEMENT -> checkedMovement
            WidgetType.VOLTAGE -> checkedVoltage
            WidgetType.SIGNAL_STRENGTH -> checkedSignalStrength
            WidgetType.ACCELERATION_X -> checkedAccelerationX
            WidgetType.ACCELERATION_Y -> checkedAccelerationY
            WidgetType.ACCELERATION_Z -> checkedAccelerationZ
        }
    }

    companion object {
        val defaultSet = setOf<WidgetType>(
            WidgetType.TEMPERATURE,
            WidgetType.HUMIDITY,
            WidgetType.PRESSURE,
            WidgetType.MOVEMENT,
            WidgetType.VOLTAGE,
            WidgetType.SIGNAL_STRENGTH
        )
    }
}

data class ComplexWidgetConfigureViewModelArgs (val appWidgetId: Int)
