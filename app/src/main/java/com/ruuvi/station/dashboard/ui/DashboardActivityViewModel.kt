package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.*
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.app.permissions.PermissionLogicInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val permissionLogicInteractor: PermissionLogicInteractor,
    private val networkApplicationSettings: NetworkApplicationSettings,
    private val networkSignInInteractor: NetworkSignInInteractor,
    private val nfcResultInteractor: NfcResultInteractor,
    private val sortingInteractor: SensorsSortingInteractor,
    private val runtimeBehavior: RuntimeBehavior
) : ViewModel() {

    private val _sensorsList = MutableStateFlow(tagInteractor.getTags())
    val sensorsList: StateFlow<List<RuuviTag>> = _sensorsList

    private var _dashBoardType =
        MutableStateFlow<DashboardType>(preferencesRepository.getDashboardType())
    val dashboardType: StateFlow<DashboardType> = _dashBoardType

    private var _dashBoardTapAction =
        MutableStateFlow<DashboardTapAction>(preferencesRepository.getDashboardTapAction())
    val dashboardTapAction: StateFlow<DashboardTapAction> = _dashBoardTapAction

    private var manualSyncJob: Job? = null

    val syncEvents = networkDataSyncInteractor.syncEvents

    val syncInProgress = networkDataSyncInteractor.syncInProgressFlow

    private val _dataRefreshing = MutableStateFlow<Boolean>(false)
    val dataRefreshing: StateFlow<Boolean> = _dataRefreshing

    val signedInOnce: StateFlow<Boolean> = MutableStateFlow<Boolean>(preferencesRepository.getSignedInOnce())

    val _bannerDisabled: MutableStateFlow<Boolean> = MutableStateFlow<Boolean>(preferencesRepository.isBannerDisabled())
    val bannerDisabled: StateFlow<Boolean> = _bannerDisabled

    val userEmail = preferencesRepository.getUserEmailLiveData()

    val _shouldAskNotificationPermission: MutableStateFlow<Boolean> = MutableStateFlow<Boolean>(permissionLogicInteractor.shouldAskNotificationPermission())
    val shouldAskNotificationPermission: StateFlow<Boolean> = _shouldAskNotificationPermission

    val shouldAskForBackgroundLocationPermission
        get() = permissionLogicInteractor.shouldAskForBackgroundLocationPermission()

    val shouldAskToEnableBluetooth
        get() = !preferencesRepository.isCloudModeEnabled() || !preferencesRepository.signedIn()

    fun refreshDashboardType() {
        _dashBoardType.value = preferencesRepository.getDashboardType()
    }

    fun refreshDashboardTapAction() {
        _dashBoardTapAction.value = preferencesRepository.getDashboardTapAction()
    }

    fun refreshNotificationStatus() {
        _shouldAskNotificationPermission.value = permissionLogicInteractor.shouldAskForBackgroundLocationPermission()
    }

    fun disableBanner() {
        preferencesRepository.disableBanner()
        _bannerDisabled.value = preferencesRepository.isBannerDisabled()
    }

    fun moveItem(from: Int, to: Int, save: Boolean = false) {
        val currentList = _sensorsList.value.toMutableList()
        val swapped = currentList.swap(from, to)
        sortingInteractor.newOrder(swapped.map { it.id })
        _sensorsList.value = swapped

        if (save) networkApplicationSettings.updateSensorsOrder()
        val sortingOrder = preferencesRepository.getSortedSensors()

        for (sens in sortingOrder) {
            Timber.d("dragGestureHandler - sortedResult $sens")
        }
        Timber.d("dragGestureHandler - sortedResult =========================")
    }

    fun onDoneDragging() {
        networkApplicationSettings.updateSensorsOrder()
    }

    fun clearSensorOrder() {
        sortingInteractor.newOrder(emptyList())
        networkApplicationSettings.updateSensorsOrder()
    }

    fun signOut() {
        networkSignInInteractor.signOut { }
    }

    fun syncCloud() {
        viewModelScope.launch {
            _dataRefreshing.value = true
            val job = networkDataSyncInteractor.syncNetworkData()
            job.invokeOnCompletion {
                _dataRefreshing.value = false
            }
            delay(200)
            if (job.isActive) _dataRefreshing.value = false
        }
    }

    fun changeDashboardType(dashboardType: DashboardType) {
        preferencesRepository.updateDashboardType(dashboardType)
        networkApplicationSettings.updateDashboardType()
        _dashBoardType.value = preferencesRepository.getDashboardType()
    }

    fun changeDashboardTapAction(dashboardTapAction: DashboardTapAction) {
        preferencesRepository.updateDashboardTapAction(dashboardTapAction)
        networkApplicationSettings.updateDashboardTapAction()
        _dashBoardTapAction.value = preferencesRepository.getDashboardTapAction()
    }

    fun getNfcScanResponse(scanInfo: SensorNfсScanInfo) =
        nfcResultInteractor.getNfcScanResponse(scanInfo)

    fun addSensor(sensorId: String) {
        tagInteractor.makeSensorFavorite(sensorId)
    }

    fun setName(sensorId: String, name: String?) {
        tagInteractor.updateTagName(sensorId, name)
        networkInteractor.updateSensorName(sensorId)
    }

    fun refreshSensors() {
        _sensorsList.value = tagInteractor.getTags()
    }

    fun isCustomOrderEnabled() = sortingInteractor.isCustomOrderEnabled()
}