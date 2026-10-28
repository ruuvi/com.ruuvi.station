package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.*
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.app.permissions.PermissionLogicInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.dashboard.DashboardType
import com.ruuvi.station.dashboard.domain.SensorsSortingInteractor
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.feature.data.FeatureFlag
import com.ruuvi.station.image.ImageInteractor
import com.ruuvi.station.network.data.response.MarketingConsentStatus
import com.ruuvi.station.network.domain.NetworkApplicationSettings
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkSettingNames
import com.ruuvi.station.network.domain.NetworkSignInInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.network.domain.MarketingConsentInteractor
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.tag.domain.isAir
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
    private val runtimeBehavior: RuntimeBehavior,
    private val bluetoothInteractor: BluetoothInteractor,
    private val marketingConsentInteractor: MarketingConsentInteractor,
    private val imageInteractor: ImageInteractor
) : ViewModel() {

    private val _sensorsList = MutableStateFlow(tagInteractor.getTags())
    val sensorsList: StateFlow<List<RuuviTag>> = _sensorsList
    private var sensorListScrolling = false
    private var sensorListDragging = false
    private var sensorListOrderGeneration = 0
    private var pendingSensorsList: List<RuuviTag>? = null

    private val sensorUpdatesPaused: Boolean
        get() = sensorListScrolling || sensorListDragging

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
    val marketingConsent = preferencesRepository.getMarketingConsentLiveData()

    private val _marketingConsentPrompt = MutableStateFlow<MarketingConsentPrompt>(MarketingConsentPrompt.Hidden)
    val marketingConsentPrompt: StateFlow<MarketingConsentPrompt> = _marketingConsentPrompt

    init {
        if (networkInteractor.signedIn && marketingConsentInteractor.isEnabled()) {
            viewModelScope.launch {
                try {
                    val consent = marketingConsentInteractor.refresh() ?: return@launch
                    if (
                        consent.consent ||
                        consent.consentStatus !in MARKETING_CONSENT_PROMPT_ELIGIBLE_STATUSES ||
                        !shouldShowMarketingConsentPrompt()
                    ) {
                        return@launch
                    }

                    _marketingConsentPrompt.value = MarketingConsentPrompt.Ask
                } catch (exception: Exception) {
                    Timber.e(exception, "Unable to refresh marketing consent")
                }
            }
        }
    }

    fun isMarketingConsentEnabled(): Boolean =
        runtimeBehavior.isFeatureEnabled(FeatureFlag.MARKETING_CONSENT)

    fun respondToMarketingConsentPrompt(consent: Boolean) {
        if (!consent) {
            preferencesRepository.setMarketingConsentPromptLastShown(System.currentTimeMillis())
            _marketingConsentPrompt.value = MarketingConsentPrompt.Hidden
            return
        }

        when (_marketingConsentPrompt.value) {
            MarketingConsentPrompt.Ask,
            MarketingConsentPrompt.Error -> Unit
            else -> return
        }

        _marketingConsentPrompt.value = MarketingConsentPrompt.Saving
        viewModelScope.launch {
            try {
                if (marketingConsentInteractor.update(true)?.consent == true) {
                    preferencesRepository.setMarketingConsentPromptLastShown(System.currentTimeMillis())
                    _marketingConsentPrompt.value = MarketingConsentPrompt.Saved
                } else {
                    _marketingConsentPrompt.value = MarketingConsentPrompt.Error
                }
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                Timber.e(exception, "Unable to save marketing consent")
                _marketingConsentPrompt.value = MarketingConsentPrompt.Error
            }
        }
    }

    fun dismissMarketingConsentSavedMessage() {
        _marketingConsentPrompt.value = MarketingConsentPrompt.Hidden
    }

    private fun shouldShowMarketingConsentPrompt(now: Long = System.currentTimeMillis()): Boolean {
        val lastShown = preferencesRepository.getMarketingConsentPromptLastShown()
        return lastShown == 0L || now - lastShown >= marketingConsentPromptIntervalMillis()
    }

    private fun marketingConsentPromptIntervalMillis(): Long {
        val developerIntervalSeconds = preferencesRepository
            .getMarketingConsentPromptIntervalSeconds()
            .takeIf { it > 0L }
        return developerIntervalSeconds
            ?.coerceAtMost(Long.MAX_VALUE / 1_000L)
            ?.times(1_000L)
            ?: MARKETING_CONSENT_PROMPT_INTERVAL_MILLIS
    }

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

            try {
                if (bluetoothInteractor.canScan()) {
                    bluetoothInteractor.startForegroundScanning()
                }

                if (preferencesRepository.signedIn()) {
                    networkDataSyncInteractor.syncNetworkData().join()
                } else {
                    delay(1000)
                }
                refreshSensors()
            } finally {
                _dataRefreshing.value = false
            }
        }
    }

    fun changeDashboardType(dashboardType: DashboardType) {
        preferencesRepository.updateDashboardType(dashboardType)
        networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.DASHBOARD_TYPE)
        _dashBoardType.value = preferencesRepository.getDashboardType()
    }

    fun changeDashboardTapAction(dashboardTapAction: DashboardTapAction) {
        preferencesRepository.updateDashboardTapAction(dashboardTapAction)
        networkApplicationSettings.updateNetworkSetting(NetworkSettingNames.DASHBOARD_TAP_ACTION)
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

    suspend fun refreshSensors() {
        val orderGeneration = sensorListOrderGeneration
        val refreshedSensors = withContext(Dispatchers.IO) {
            tagInteractor.getTags()
        }

        if (orderGeneration != sensorListOrderGeneration) return

        if (sensorUpdatesPaused) {
            pendingSensorsList = refreshedSensors
        } else {
            pendingSensorsList = null
            _sensorsList.value = refreshedSensors
        }
    }

    fun setSensorListScrolling(scrolling: Boolean) {
        sensorListScrolling = scrolling
        publishPendingSensorsIfResumed()
    }

    fun setSensorListDragging(dragging: Boolean) {
        val dragStarted = !sensorListDragging && dragging
        val dragEnded = sensorListDragging && !dragging
        sensorListDragging = dragging

        if (dragStarted) {
            sensorListOrderGeneration++
        }

        if (dragEnded) {
            // Invalidate refreshes started during the drag so they cannot restore
            // an order read before the final dragged order was persisted.
            sensorListOrderGeneration++
            // The pending list may contain the order from before the drag started.
            pendingSensorsList = null
        }

        publishPendingSensorsIfResumed()
    }

    private fun publishPendingSensorsIfResumed() {
        if (!sensorUpdatesPaused) {
            pendingSensorsList?.let { sensors ->
                pendingSensorsList = null
                _sensorsList.value = sensors
            }
        }
    }

    fun isCustomOrderEnabled() = sortingInteractor.isCustomOrderEnabled()

    fun getDefaultImageResource(sensor: RuuviTag): Int {
        return imageInteractor.getDefaultBackgroundById(sensor.defaultBackground, sensor.isAir())
    }

    private companion object {
        const val MARKETING_CONSENT_PROMPT_INTERVAL_MILLIS = 180L * 24 * 60 * 60 * 1000
        val MARKETING_CONSENT_PROMPT_ELIGIBLE_STATUSES = setOf(
            MarketingConsentStatus.UNSUBSCRIBED,
            MarketingConsentStatus.NOT_FOUND
        )
    }
}

sealed interface MarketingConsentPrompt {
    data object Hidden : MarketingConsentPrompt
    data object Ask : MarketingConsentPrompt
    data object Saving : MarketingConsentPrompt
    data object Error : MarketingConsentPrompt
    data object Saved : MarketingConsentPrompt
}
