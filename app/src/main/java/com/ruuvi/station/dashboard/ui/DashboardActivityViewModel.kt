package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.*
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Utils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import java.util.*

@ExperimentalCoroutinesApi
class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter,
    val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val tags = MutableLiveData<List<RuuviTag>>(arrayListOf())
    val observeTags: LiveData<List<RuuviTag>> = tags

    private val syncResult = MutableLiveData<String>("")
    val syncResultObserve: LiveData<String> = syncResult

    private val syncInProgress = MutableLiveData<Boolean>(false)
    val syncInProgressObserve: LiveData<Boolean> = syncInProgress

    val userEmail = preferencesRepository.getUserEmailLiveData()

    val lastSync = preferencesRepository.getLastSyncDateLiveData()

    val syncStatus: MediatorLiveData<String> = MediatorLiveData<String>()

    private val trigger = MutableLiveData<Int>(1)

    init {
        updateTags()
        Timber.d("DashboardActivityViewModel-syncStatusFlow")

        viewModelScope.launch {
            networkDataSyncInteractor.syncStatusFlow.collect {
                syncResult.value = it
            }
        }
        viewModelScope.launch {
            networkDataSyncInteractor.syncInProgressFlow.collect{
                syncInProgress.value = it
                updateTags()
            }
        }

        syncStatus.addSource(syncInProgress) { syncStatus.value = updateSyncStatus() }
        syncStatus.addSource(lastSync) { syncStatus.value = updateSyncStatus() }
        syncStatus.addSource(trigger) { syncStatus.value = updateSyncStatus() }
    }

    private fun updateSyncStatus(): String {
        val syncInProgress = syncInProgress.value ?: false
        val lastSync = lastSync.value ?: Long.MIN_VALUE

        if (syncInProgress) {
            return "Synchronizing..."
        } else {
            return if (lastSync == Long.MIN_VALUE) {
                "Synchronized: never"
            } else {
                "Synchronized: ${Utils.strDescribingTimeSince(Date(lastSync))}"
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

    fun syncResultShowed() {
        networkDataSyncInteractor.syncStatusShowed()
    }

    fun updateNetworkStatus() {
        CoroutineScope(Dispatchers.Main).launch {
            trigger.value = -1
        }
    }
}