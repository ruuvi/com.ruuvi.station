package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.*
import com.ruuvi.station.app.permissions.PermissionLogicInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    val preferencesRepository: PreferencesRepository,
    private val tokenRepository: NetworkTokenRepository,
    private val permissionLogicInteractor: PermissionLogicInteractor
) : ViewModel() {

    val tagsFlow: Flow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    private var _dashBoardType = MutableStateFlow<DashboardType> (preferencesRepository.getDashboardType())
    val dashboardType: StateFlow<DashboardType> = _dashBoardType

    val syncEvents = networkDataSyncInteractor.syncEvents

    val userEmail = preferencesRepository.getUserEmailLiveData()

    val shouldAskNotificationPermission
        get() = permissionLogicInteractor.shouldAskNotificationPermission()

    val shouldAskForBackgroundLocationPermission
        get() = permissionLogicInteractor.shouldAskForBackgroundLocationPermission()

    fun signOut() {
        networkDataSyncInteractor.stopSync()
        tokenRepository.signOut { }
    }

    fun changeDashboardType(dashboardType: DashboardType) {
        preferencesRepository.updateDashboardType(dashboardType)
        _dashBoardType.value = preferencesRepository.getDashboardType()
    }
}