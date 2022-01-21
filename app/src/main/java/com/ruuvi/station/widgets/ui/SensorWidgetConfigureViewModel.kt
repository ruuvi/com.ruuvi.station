package com.ruuvi.station.widgets.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.tag.domain.RuuviTag

class SensorWidgetConfigureViewModel(
    private val tagRepository: TagRepository
): ViewModel() {

    private val _sensors = MutableLiveData<List<RuuviTag>> (tagRepository.getFavoriteSensors())
    val sensors: LiveData<List<RuuviTag>> = _sensors
}