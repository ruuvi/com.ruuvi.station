package com.ruuvi.station.widgets.ui.simpleWidget

import android.appwidget.AppWidgetManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import timber.log.Timber

class SimpleWidgetConfigureViewModel(
    private val tagRepository: TagRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val widgetPreferencesInteractor: WidgetPreferencesInteractor
): ViewModel() {
    var appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private val _sensors = MutableLiveData<List<RuuviTag>> (tagRepository.getFavoriteSensors().filter { it.networkLastSync != null })
    val sensors: LiveData<List<RuuviTag>> = _sensors

    private val _userLoggedIn = MutableLiveData<Boolean> (networkInteractor.signedIn)
    val userLoggedIn: LiveData<Boolean> = _userLoggedIn

    private val _setupComplete = MutableLiveData<Boolean> (false)
    val setupComplete: LiveData<Boolean> = _setupComplete

    private val _sensorId = MutableLiveData<String?> (null)
    val sensorId: LiveData<String?> = _sensorId

    private val _widgetType = MutableLiveData<WidgetType> (DEFAULT_WIDGET_TYPE)
    val widgetType: LiveData<WidgetType> = _widgetType

    fun setWidgetId(appWidgetId: Int) {
        this.appWidgetId = appWidgetId
        _sensorId.value = widgetPreferencesInteractor.getSimpleWidgetSensor(appWidgetId)
        _widgetType.value = widgetPreferencesInteractor.getSimpleWidgetType(appWidgetId)
        Timber.d("setWidgetId $appWidgetId ${_sensorId.value} ${_widgetType.value}")
    }

    fun selectSensor(sensorId: String) {
        _sensorId.value = sensorId
    }

    fun selectWidgetType(widgetType: WidgetType) {
        _widgetType.value = widgetType
    }

    fun saveSettings() {
        val sensor = sensorId.value
        if (!sensor.isNullOrEmpty()) {
            widgetPreferencesInteractor.saveSimpleWidgetSettings(appWidgetId, sensor, _widgetType.value ?: DEFAULT_WIDGET_TYPE )
            _setupComplete.value = true
        }
    }

    companion object {
        val DEFAULT_WIDGET_TYPE = WidgetType.TEMPERATURE
    }
}