package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.*
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.app.permissions.PermissionLogicInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    private val networkInteractor: RuuviNetworkInteractor,
    private val preferencesRepository: PreferencesRepository,
    private val permissionLogicInteractor: PermissionLogicInteractor,
    private val networkApplicationSettings: NetworkApplicationSettings,
    private val networkSignInInteractor: NetworkSignInInteractor,
    private val nfcResultInteractor: NfcResultInteractor
) : ViewModel() {

    val tagsFlow: Flow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private var _dashBoardType =
        MutableStateFlow<DashboardType>(preferencesRepository.getDashboardType())
    val dashboardType: StateFlow<DashboardType> = _dashBoardType

    private var _dashBoardTapAction =
        MutableStateFlow<DashboardTapAction>(preferencesRepository.getDashboardTapAction())
    val dashboardTapAction: StateFlow<DashboardTapAction> = _dashBoardTapAction

    private var manualSyncJob: Job? = null

    val syncEvents = networkDataSyncInteractor.syncEvents

    private val _dataRefreshing = MutableStateFlow<Boolean>(false)
    val dataRefreshing: StateFlow<Boolean> = _dataRefreshing

    val userEmail = preferencesRepository.getUserEmailLiveData()

    val shouldAskNotificationPermission
        get() = permissionLogicInteractor.shouldAskNotificationPermission()

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

    fun signOut() {
        networkDataSyncInteractor.stopSync()
        networkSignInInteractor.signOut { }
    }

    fun syncCloud() {
        viewModelScope.launch {
            _dataRefreshing.value = true
            val job = networkDataSyncInteractor.syncNetworkData()
            job.invokeOnCompletion {
                _dataRefreshing.value = false
            }
            delay(5000)
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
        tagInteractor.getTagEntityById(sensorId)?.let {
            tagInteractor.makeSensorFavorite(it)
        }
    }

    fun setName(sensorId: String, name: String?) {
        tagInteractor.updateTagName(sensorId, name)
        networkInteractor.updateSensorName(sensorId)
    }
}