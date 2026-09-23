package com.ruuvi.station.widgets.ui.simpleWidget

import android.appwidget.AppWidgetManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.ICloudWidgetViewModel
import timber.log.Timber

class SimpleWidgetConfigureViewModel(
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val widgetPreferencesInteractor: WidgetPreferencesInteractor,
    private val preferencesRepository: PreferencesRepository
): ViewModel(), ICloudWidgetViewModel {
    var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private val _allSensors = MutableLiveData<List<RuuviTag>>(tagRepository.getFavoriteSensors())
    val allSensors: LiveData<List<RuuviTag>> = _allSensors

    val gotLocalSensors = _allSensors.map { allSensors ->
        allSensors.any { it.networkLastSync == null }
    }

    override val userLoggedIn: LiveData<Boolean> = MutableLiveData<Boolean> (networkInteractor.signedIn)

    private val _setupComplete = MutableLiveData<Boolean> (false)
    val setupComplete: LiveData<Boolean> = _setupComplete

    private val _sensorId = MutableLiveData<String?> (null)
    val sensorId: LiveData<String?> = _sensorId

    private val _widgetType = MutableLiveData<WidgetType?>(null)
    val widgetType: LiveData<WidgetType?> = _widgetType

    override val userHasCloudSensors: LiveData<Boolean> = allSensors.map { allSensors ->
        allSensors.any { it.networkLastSync != null }
    }

    val backgroundServiceInterval = preferencesRepository.getBackgroundScanInterval()

    private val _backgroundServiceEnabled: MutableLiveData<Boolean> = MutableLiveData<Boolean>(preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND)
    val backgroundServiceEnabled: LiveData<Boolean> = _backgroundServiceEnabled

    private val _canBeSaved = MutableLiveData(false)
    override val canBeSaved: LiveData<Boolean> = _canBeSaved

    fun setWidgetId(appWidgetId: Int) {
        this.appWidgetId = appWidgetId
        val sensorId = widgetPreferencesInteractor.getSimpleWidgetSensor(appWidgetId)
        val widgetType = widgetPreferencesInteractor.getSimpleWidgetType(appWidgetId)
        updateSelection(findSensor(sensorId), widgetType)
        Timber.d("setWidgetId $appWidgetId ${_sensorId.value} ${_widgetType.value}")
    }

    fun selectSensor(sensorId: String) {
        updateSelection(findSensor(sensorId), _widgetType.value)
    }

    fun selectWidgetType(widgetType: WidgetType) {
        val sensor = findSensor(_sensorId.value) ?: return
        if (widgetType in WidgetType.filterWidgetTypes(sensor)) {
            _widgetType.value = widgetType
            updateCanBeSaved()
        }
    }

    override fun save() {
        val selection = getValidSelection()
        if (selection == null) {
            _canBeSaved.value = false
            return
        }

        widgetPreferencesInteractor.saveSimpleWidgetSettings(
            appWidgetId,
            selection.sensor.id,
            selection.widgetType,
        )
        _setupComplete.value = true
    }

    fun enableBackgroundService() {
        preferencesRepository.setBackgroundScanMode(BackgroundScanModes.BACKGROUND)
        _backgroundServiceEnabled.value = preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND
    }

    private fun updateSelection(sensor: RuuviTag?, preferredType: WidgetType?) {
        val supportedTypes = sensor?.let(WidgetType::filterWidgetTypes).orEmpty()
        _sensorId.value = sensor?.id
        _widgetType.value = chooseSuitableWidgetType(preferredType, supportedTypes)
        updateCanBeSaved()
    }

    private fun updateCanBeSaved() {
        _canBeSaved.value = getValidSelection() != null
    }

    private fun getValidSelection(): ValidSelection? {
        val sensor = findSensor(_sensorId.value) ?: return null
        val widgetType = _widgetType.value ?: return null
        return if (widgetType in WidgetType.filterWidgetTypes(sensor)) {
            ValidSelection(sensor, widgetType)
        } else {
            null
        }
    }

    private fun findSensor(sensorId: String?): RuuviTag? =
        _allSensors.value.orEmpty().firstOrNull { it.id == sensorId }

    companion object {
        val DEFAULT_WIDGET_TYPE = WidgetType.TEMPERATURE

        internal fun chooseSuitableWidgetType(
            preferredType: WidgetType?,
            supportedTypes: List<WidgetType>,
        ): WidgetType? =
            preferredType?.takeIf { it in supportedTypes }
                ?: DEFAULT_WIDGET_TYPE.takeIf { it in supportedTypes }
                ?: supportedTypes.firstOrNull()
    }

    private data class ValidSelection(
        val sensor: RuuviTag,
        val widgetType: WidgetType,
    )
}
