package com.ruuvi.station.network.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClaimSensorViewModel (
    val sensorId: String,
    private val ruuviNetworkInteractor: RuuviNetworkInteractor,
    private val interactor: TagSettingsInteractor,
    ): ViewModel() {

    private val claimResult = MutableLiveData<Pair<Boolean, String>?> (null)
    val claimResultObserve: LiveData<Pair<Boolean, String>?> = claimResult

    fun claimSensor() {
        CoroutineScope(Dispatchers.IO).launch {
            val settings = interactor.getSensorSettings(sensorId)
            settings?.let {
                ruuviNetworkInteractor.claimSensor(settings) {
                    claimResult.value = Pair(it?.isSuccess() ?: false, it?.error ?: "")
                }
            }
        }
    }
}