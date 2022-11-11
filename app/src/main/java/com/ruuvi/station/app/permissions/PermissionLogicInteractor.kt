package com.ruuvi.station.app.permissions

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.util.BackgroundScanModes

class PermissionLogicInteractor(
    private val alarmRepository: AlarmRepository,
    private val preferencesRepository: PreferencesRepository
) {
    private val backGroundScanningEnabled
        get() = preferencesRepository.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND

    private val alarmsExists
        get() = alarmRepository.getActiveAlarms().any()

    fun shouldAskNotificationPermission(): Boolean = backGroundScanningEnabled || alarmsExists

    fun shouldAskForBackgroundLocation(): Boolean = backGroundScanningEnabled
}