package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter,
    val networkDataSyncInteractor: NetworkDataSyncInteractor
) : ViewModel() {

    private val tags = MutableLiveData<List<RuuviTag>>(arrayListOf())
    val observeTags: LiveData<List<RuuviTag>> = tags

    private val syncStatus = MutableLiveData<String>("")
    val syncStatusObserve: LiveData<String> = syncStatus

    private val syncInProgress = MutableLiveData<Boolean>(false)
    val syncInProgressObserve: LiveData<Boolean> = syncInProgress

    init {
        updateTags()

        viewModelScope.launch {
            networkDataSyncInteractor.syncStatusFlow.collect {
                syncStatus.value = it
            }
        }
        viewModelScope.launch {
            networkDataSyncInteractor.syncInProgressFlow.collect{
                syncInProgress.value = it
            }
        }
    }

    fun updateTags() {
        viewModelScope.launch {
            val getTags = tagInteractor.getTags()
            withContext(Dispatchers.Main) {
                tags.value = getTags
            }
        }
    }

    fun networkDataSync() {
        networkDataSyncInteractor.syncNetworkData()
    }
}