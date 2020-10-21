package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter
) : ViewModel() {

    private val tags = MutableLiveData<List<RuuviTag>>(arrayListOf())
    val observeTags: LiveData<List<RuuviTag>> = tags

    init {
        updateTags()
    }

    fun updateTags() {
        viewModelScope.launch {
            val getTags = tagInteractor.getTags()
            withContext(Dispatchers.Main) {
                tags.value = getTags
            }
        }
    }
}