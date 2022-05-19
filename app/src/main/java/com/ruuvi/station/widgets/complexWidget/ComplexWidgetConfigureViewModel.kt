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
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferenceItem
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.ICloudWidgetViewModel
import timber.log.Timber

class ComplexWidgetConfigureViewModel(
    private val appWidgetId: Int,
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesInteractor: ComplexWidgetPreferencesInteractor
    ): ViewModel(), ICloudWidgetViewModel {

    private val _allSensors = MutableLiveData<List<RuuviTag>> (tagRepository.getFavoriteSensors())

    private val cloudSensors = Transformations.map(_allSensors) { allSensors ->
        allSensors.filter { it.networkLastSync != null }
    }

    val gotFilteredSensors = Transformations.map(_allSensors) { allSensors ->
        allSensors.any { it.networkLastSync == null }
    }

    private val _widgetItems = MutableLiveData(getSensorsForWidgetId(appWidgetId))
    val widgetItems: LiveData<List<ComplexWidgetSensorItem>> = _widgetItems

    private val _canBeSaved = MutableLiveData(false)
    override val canBeSaved: LiveData<Boolean> = _canBeSaved

    override val userLoggedIn: LiveData<Boolean> = MutableLiveData<Boolean> (networkInteractor.signedIn)

    override val userHasCloudSensors: LiveData<Boolean> = Transformations.map(cloudSensors) {
        it.isNotEmpty()
    }

    private val _setupComplete = MutableLiveData<Boolean> (false)
    val setupComplete: LiveData<Boolean> = _setupComplete

    init {
        recalcCanBeSaved()
    }

    override fun save() {
        _widgetItems.value?.let {
            preferencesInteractor.saveComplexWidgetSettings(appWidgetId, it)
            _setupComplete.value = true
        }
    }

    fun getSensorsForWidgetId(appWidgetId: Int): List<ComplexWidgetSensorItem> {
        val saved = preferencesInteractor.getComplexWidgetSettings(appWidgetId)
        return tagRepository.getFavoriteSensors().filter { it.networkLastSync != null }.map { cloudSensor ->
            ComplexWidgetSensorItem(cloudSensor.id, cloudSensor.displayName).also {
                it.restoreSettings(saved.firstOrNull { it.sensorId == cloudSensor.id })
            }
        }
    }

    fun selectSensor(item: ComplexWidgetSensorItem, checked: Boolean) {
        Timber.d("selectSensor ${item.sensorId}")
        _widgetItems.value?.find { it.sensorId == item.sensorId }?.let {
            it.checked = checked
        }
        recalcCanBeSaved()
    }

    fun selectWidgetType(item: ComplexWidgetSensorItem, widgetType: WidgetType, checked: Boolean) {
        _widgetItems.value?.find { it.sensorId == item.sensorId }?.let {
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
        recalcCanBeSaved()
    }

    private fun recalcCanBeSaved() {
        _canBeSaved.value = _widgetItems?.value?.any { item -> item.checked && item.anySensorChecked() } ?: false
    }
}

class ComplexWidgetSensorItem(
    val sensorId: String,
    var sensorName: String
) {
    var checked by mutableStateOf(false)

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

    fun anySensorChecked(): Boolean = checkedTemperature || checkedHumidity || checkedPressure ||
                checkedMovement || checkedVoltage || checkedSignalStrength ||
                checkedAccelerationX || checkedAccelerationY || checkedAccelerationZ

    fun restoreSettings(savedState: ComplexWidgetPreferenceItem?) {
        checked = savedState != null
        checkedTemperature = savedState?.checkedTemperature ?: checkedTemperatureDefault
        checkedHumidity = savedState?.checkedHumidity ?: checkedHumidityDefault
        checkedPressure = savedState?.checkedPressure ?: checkedPressureDefault
        checkedMovement = savedState?.checkedMovement ?: checkedMovementDefault
        checkedVoltage = savedState?.checkedVoltage ?: checkedVoltageDefault
        checkedSignalStrength = savedState?.checkedSignalStrength ?: checkedSignalStrengthDefault
        checkedAccelerationX = savedState?.checkedAccelerationX ?: checkedAccelerationXDefault
        checkedAccelerationY = savedState?.checkedAccelerationY ?: checkedAccelerationYDefault
        checkedAccelerationZ = savedState?.checkedAccelerationZ ?: checkedAccelerationZDefault
    }

    companion object {
        const val checkedTemperatureDefault = true
        const val checkedHumidityDefault = true
        const val checkedPressureDefault = true
        const val checkedMovementDefault = true
        const val checkedVoltageDefault = true
        const val checkedSignalStrengthDefault = true
        const val checkedAccelerationXDefault = false
        const val checkedAccelerationYDefault = false
        const val checkedAccelerationZDefault = false
    }
}

data class ComplexWidgetConfigureViewModelArgs (val appWidgetId: Int)
