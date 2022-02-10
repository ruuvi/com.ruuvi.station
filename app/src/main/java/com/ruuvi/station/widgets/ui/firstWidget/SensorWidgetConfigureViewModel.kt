package com.ruuvi.station.widgets.ui.firstWidget

import android.appwidget.AppWidgetManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor

class SensorWidgetConfigureViewModel(
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

    fun setWidgetId(appWidgetId: Int) {
        this.appWidgetId = appWidgetId
    }

    fun saveWidgetSettings(sensorId: String) {
        widgetPreferencesInteractor.saveWidgetSettings(appWidgetId, sensorId)
        _setupComplete.value = true
    }
}