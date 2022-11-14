package com.ruuvi.station.tagdetails.ui

import androidx.lifecycle.*
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmStatus
import com.ruuvi.station.app.permissions.PermissionLogicInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.NetworkSyncStatus
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tagdetails.domain.TagDetailsArguments
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.ruuvi.station.network.domain.NetworkApplicationSettings

class TagDetailsViewModel(
    tagDetailsArguments: TagDetailsArguments,
    private val interactor: TagInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val tokenRepository: NetworkTokenRepository,
    private val networkApplicationSettings: NetworkApplicationSettings,
    private val permissionLogicInteractor: PermissionLogicInteractor
) : ViewModel() {

    private val ioScope = CoroutineScope(Dispatchers.IO)

    val shouldAskNotificationPermission
        get() = permissionLogicInteractor.shouldAskNotificationPermission()

    val shouldAskForBackgroundLocationPermission
        get() = permissionLogicInteractor.shouldAskForBackgroundLocationPermission()

    private val isShowGraph = MutableLiveData<Boolean>(false)
    val isShowGraphObserve: LiveData<Boolean> = isShowGraph

    private val selectedTag = MutableLiveData<RuuviTag?>(null)
    val selectedTagObserve: LiveData<RuuviTag?> = selectedTag

    private val tags = MutableLiveData<List<RuuviTag>>(interactor.getTags())
    val tagsObserve: LiveData<List<RuuviTag>> = tags

    private val alarmStatus = MutableLiveData(AlarmStatus.NO_ALARM)
    val alarmStatusObserve: LiveData<AlarmStatus> = alarmStatus

    var dashboardEnabled = isDashboardEnabled()

    private var prevTag: RuuviTag? = null
    fun getPrevTag() = prevTag

    var openAddView: Boolean = tagDetailsArguments.shouldOpenAddView && interactor.getTagEntities(true).isEmpty()
    var desiredTag: String? = tagDetailsArguments.desiredTag

    val syncEvents = networkDataSyncInteractor.syncEvents

    private val syncInProgress = MutableLiveData<Boolean>(false)
    val syncInProgressObserve: LiveData<Boolean> = syncInProgress

    val userEmail = preferencesRepository.getUserEmailLiveData()

    val lastSync = preferencesRepository.getLastSyncDateLiveData()

    val syncStatus:MediatorLiveData<NetworkSyncStatus>  = MediatorLiveData<NetworkSyncStatus>()

    private val trigger = MutableLiveData<Int>(1)

    init {
        viewModelScope.launch {
            networkDataSyncInteractor.syncInProgressFlow.collect{
                syncInProgress.value = it
                if (it == false) refreshTags()
            }
        }

        syncStatus.addSource(syncInProgress) { syncStatus.value = getSyncStatus() }
        syncStatus.addSource(lastSync) { syncStatus.value = getSyncStatus() }
        syncStatus.addSource(trigger) { syncStatus.value = getSyncStatus() }
    }

    private fun getSyncStatus(): NetworkSyncStatus = NetworkSyncStatus(
        syncInProgress.value ?: false,
        lastSync.value ?: Long.MIN_VALUE
    )

    fun pageSelected(pageIndex: Int) {
        viewModelScope.launch {
            Timber.d("pageSelected $pageIndex")
            prevTag = selectedTag.value
            val tag = tags.value?.get(pageIndex)
            selectedTag.value = tag
            desiredTag = tag?.id
            checkForAlarm()
        }
    }

    fun switchShowGraphChannel() {
        isShowGraph.value = !(isShowGraph.value ?: true)
    }

    fun refreshTags() {
        ioScope.launch {
            val list = interactor.getTags()
            val sensorListChanged = sensorListChanged(tags.value ?: listOf(), list)
            Timber.d("sensorListChanged = $sensorListChanged")
            if (sensorListChanged) {
                withContext(Dispatchers.Main) {
                    tags.value = list
                }
            }
        }
    }

    fun getBackgroundScanMode(): BackgroundScanModes =
        interactor.getBackgroundScanMode()

    fun setBackgroundScanMode(mode: BackgroundScanModes) {
        interactor.setBackgroundScanMode(mode)
        networkApplicationSettings.updateBackgroundScanMode()
    }

    fun isFirstGraphVisit(): Boolean =
        interactor.isFirstGraphVisit()

    fun setIsFirstGraphVisit(isFirst: Boolean) =
        interactor.setIsFirstGraphVisit(isFirst)

    fun isShowingGraph() = isShowGraph.value == true

    private fun isDashboardEnabled(): Boolean =
        interactor.isDashboardEnabled()

    fun checkForAlarm() {
        ioScope.launch {
            Timber.d("checkForAlarm")
            selectedTag.value?.id?.let { tagId ->
                val tagEntry = interactor.getTagByID(tagId)
                tagEntry?.let {
                    withContext(Dispatchers.Main) {
                        alarmStatus.value = alarmCheckInteractor.getStatus(it)
                    }
                }
            }
        }
    }

    fun updateNetworkStatus() {
        CoroutineScope(Dispatchers.Main).launch {
            trigger.value = -1
        }
    }

    fun signOut() {
        networkDataSyncInteractor.stopSync()
        tokenRepository.signOut {
            refreshTags()
        }
    }

    private fun sensorListChanged(old: List<RuuviTag>, new: List<RuuviTag>): Boolean {
        return old.any { oldTag -> new.none { it.id == oldTag.id} } ||
            new.any{ newTag -> old.none{ it.id == newTag.id}} ||
            new.any { newTag -> old.any { it.sensorListSpecificChange(newTag) } }
    }
}

fun RuuviTag.sensorListSpecificChange(newTag: RuuviTag): Boolean {
    return this.id == newTag.id &&
            (this.displayName != newTag.displayName ||
            this.defaultBackground != newTag.defaultBackground ||
            this.userBackground != newTag.userBackground)
}