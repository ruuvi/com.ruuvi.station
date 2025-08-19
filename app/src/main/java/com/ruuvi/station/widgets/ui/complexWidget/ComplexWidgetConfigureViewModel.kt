package com.ruuvi.station.widgets.complexWidget

import androidx.compose.runtime.*
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.data.WidgetType.Companion.filterWidgetTypes
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferenceItem
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.ICloudWidgetViewModel
import timber.log.Timber

class ComplexWidgetConfigureViewModel(
    private val appWidgetId: Int,
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesInteractor: ComplexWidgetPreferencesInteractor,
    private val preferencesRepository: PreferencesRepository
    ): ViewModel(), ICloudWidgetViewModel {

    private val _allSensors = MutableLiveData<List<RuuviTag>> (tagRepository.getFavoriteSensors())

    private val cloudSensors = _allSensors

    val gotLocalSensors = _allSensors.map { allSensors ->
        allSensors.any { it.networkLastSync == null }
    }

    private val _widgetItems = MutableLiveData(getSensorsForWidgetId(appWidgetId))
    val widgetItems: LiveData<List<ComplexWidgetSensorItem>> = _widgetItems

    private val _canBeSaved = MutableLiveData(false)
    override val canBeSaved: LiveData<Boolean> = _canBeSaved

    override val userLoggedIn: LiveData<Boolean> = MutableLiveData<Boolean> (networkInteractor.signedIn)

    override val userHasCloudSensors: LiveData<Boolean> = cloudSensors.map {
        it.isNotEmpty()
    }

    val backgroundServiceInterval = preferencesRepository.getBackgroundScanInterval()

    private val _backgroundServiceEnabled: MutableLiveData<Boolean> = MutableLiveData<Boolean>(preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND)
    val backgroundServiceEnabled: LiveData<Boolean> = _backgroundServiceEnabled

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
        return tagRepository.getFavoriteSensors().map { cloudSensor ->
            ComplexWidgetSensorItem(cloudSensor).also {
                it.restoreSettings(cloudSensor, saved.firstOrNull { it.sensorId == cloudSensor.id })
            }
        }
    }

    fun selectSensor(item: ComplexWidgetSensorItem, checked: Boolean) {
        Timber.d("selectSensor ${item.sensor.id}")
        _widgetItems.value?.find { it.sensor.id == item.sensor.id }?.let {
            it.checked = checked
        }
        recalcCanBeSaved()
    }

    fun selectWidgetType(item: ComplexWidgetSensorItem, widgetType: WidgetType, checked: Boolean) {
        _widgetItems.value?.find { it.sensor.id == item.sensor.id }?.let {
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
                WidgetType.AIR_QUALITY -> it.checkedAQI = checked
                WidgetType.LUMINOSITY -> it.checkedLuminosity = checked
                WidgetType.CO2 -> it.checkedCO2 = checked
                WidgetType.VOC -> it.checkedVOC = checked
                WidgetType.NOX -> it.checkedNOX = checked
                WidgetType.PM10 -> it.checkedPM10 = checked
                WidgetType.PM25 -> it.checkedPM25 = checked
                WidgetType.PM40 -> it.checkedPM40 = checked
                WidgetType.PM100 -> it.checkedPM100 = checked
            }
        }
        recalcCanBeSaved()
    }

    private fun recalcCanBeSaved() {
        _canBeSaved.value = _widgetItems?.value?.any { item -> item.checked && item.anySensorChecked() } ?: false
    }

    fun enableBackgroundService() {
        preferencesRepository.setBackgroundScanMode(BackgroundScanModes.BACKGROUND)
        _backgroundServiceEnabled.value = preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND
    }
}

class ComplexWidgetSensorItem(
    val sensor: RuuviTag
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
    var checkedAQI by mutableStateOf(false)
    var checkedLuminosity by mutableStateOf(false)
    var checkedCO2 by mutableStateOf(false)
    var checkedVOC by mutableStateOf(false)
    var checkedNOX by mutableStateOf(false)
    var checkedPM10 by mutableStateOf(false)
    var checkedPM25 by mutableStateOf(false)
    var checkedPM40 by mutableStateOf(false)
    var checkedPM100 by mutableStateOf(false)

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
            WidgetType.AIR_QUALITY -> checkedAQI
            WidgetType.LUMINOSITY -> checkedLuminosity
            WidgetType.CO2 -> checkedCO2
            WidgetType.VOC -> checkedVOC
            WidgetType.NOX -> checkedNOX
            WidgetType.PM10 -> checkedPM10
            WidgetType.PM25 -> checkedPM25
            WidgetType.PM40 -> checkedPM40
            WidgetType.PM100 -> checkedPM100
        }
    }

    fun anySensorChecked(): Boolean = checkedTemperature || checkedHumidity || checkedPressure ||
            checkedMovement || checkedVoltage || checkedSignalStrength ||
            checkedAccelerationX || checkedAccelerationY || checkedAccelerationZ || checkedAQI ||
            checkedLuminosity || checkedCO2 || checkedVOC || checkedNOX || checkedPM10 ||
            checkedPM25 || checkedPM40 || checkedPM100

    fun restoreSettings(
        sensor: RuuviTag,
        savedState: ComplexWidgetPreferenceItem?
    ) {
        val supportedType = filterWidgetTypes(sensor)
        checked = savedState != null
        checkedTemperature = savedState?.checkedTemperature
            ?: if (supportedType.any { it.unitType == UnitType.TemperatureUnit.Celsius }) checkedTemperatureDefault else false
        checkedHumidity = savedState?.checkedHumidity
            ?: if (supportedType.any { it.unitType == UnitType.HumidityUnit.Relative }) checkedHumidityDefault else false
        checkedPressure = savedState?.checkedPressure
            ?: if (supportedType.any { it.unitType == UnitType.PressureUnit.HectoPascal }) checkedPressureDefault else false
        checkedMovement = savedState?.checkedMovement
            ?: if (supportedType.any { it.unitType == UnitType.MovementUnit.MovementsCount }) checkedMovementDefault else false
        checkedVoltage = savedState?.checkedVoltage
            ?: if (supportedType.any { it.unitType == UnitType.BatteryVoltageUnit.Volt }) checkedVoltageDefault else false
        checkedSignalStrength = savedState?.checkedSignalStrength
            ?: if (supportedType.any { it.unitType == UnitType.SignalStrengthUnit.SignalDbm }) checkedSignalStrengthDefault else false
        checkedAccelerationX = savedState?.checkedAccelerationX
            ?: if (supportedType.any { it.unitType == UnitType.Acceleration.GForceX }) checkedAccelerationXDefault else false
        checkedAccelerationY = savedState?.checkedAccelerationY
            ?: if (supportedType.any { it.unitType == UnitType.Acceleration.GForceY }) checkedAccelerationYDefault else false
        checkedAccelerationZ = savedState?.checkedAccelerationZ
            ?: if (supportedType.any { it.unitType == UnitType.Acceleration.GForceZ }) checkedAccelerationZDefault else false
        checkedAQI = savedState?.checkedAQI
            ?: if (supportedType.any { it.unitType == UnitType.AirQuality.AqiIndex }) checkedAirQualityDefault else false
        checkedLuminosity = savedState?.checkedLuminosity
            ?: if (supportedType.any { it.unitType == UnitType.Luminosity.Lux }) checkedLuminosityDefault else false
        checkedCO2 = savedState?.checkedCO2
            ?: if (supportedType.any { it.unitType == UnitType.CO2.Ppm }) checkedCO2Default else false
        checkedVOC = savedState?.checkedVOC
            ?: if (supportedType.any { it.unitType == UnitType.VOC.VocIndex }) checkedVOCDefault else false
        checkedNOX = savedState?.checkedNOX
            ?: if (supportedType.any { it.unitType == UnitType.NOX.NoxIndex }) checkedNOXDefault else false
        checkedPM10 = savedState?.checkedPM10
            ?: if (supportedType.any { it.unitType == UnitType.PM10.Mgm3 }) checkedPM10Default else false
        checkedPM25 = savedState?.checkedPM25
            ?: if (supportedType.any { it.unitType == UnitType.PM25.Mgm3 }) checkedPM25Default else false
        checkedPM40 = savedState?.checkedPM40
            ?: if (supportedType.any { it.unitType == UnitType.PM40.Mgm3 }) checkedPM40Default else false
        checkedPM100 = savedState?.checkedPM100
            ?: if (supportedType.any { it.unitType == UnitType.PM100.Mgm3 }) checkedPM100Default else false
    }

    companion object {
        const val checkedTemperatureDefault = true
        const val checkedHumidityDefault = true
        const val checkedPressureDefault = true
        const val checkedMovementDefault = true
        const val checkedVoltageDefault = true
        const val checkedSignalStrengthDefault = false
        const val checkedAccelerationXDefault = false
        const val checkedAccelerationYDefault = false
        const val checkedAccelerationZDefault = false
        const val checkedAirQualityDefault = true
        const val checkedLuminosityDefault = false
        const val checkedCO2Default = true
        const val checkedVOCDefault = false
        const val checkedNOXDefault = false
        const val checkedPM10Default = false
        const val checkedPM25Default = true
        const val checkedPM40Default = false
        const val checkedPM100Default = false
    }
}

data class ComplexWidgetConfigureViewModelArgs (val appWidgetId: Int)
