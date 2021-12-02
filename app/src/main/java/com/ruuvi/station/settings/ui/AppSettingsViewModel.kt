package com.ruuvi.station.settings.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.station.R
import com.ruuvi.station.settings.domain.AppSettingsInteractor
import timber.log.Timber

class AppSettingsViewModel (
    private val interactor: AppSettingsInteractor,
    ): ViewModel()
{
    var resourceId: Int = R.string.menu_app_settings

    val isDashboardEnabled: Boolean = interactor.isDashboardEnabled()

    init {
        Timber.d("isDashboardEnabled = $isDashboardEnabled")
    }

    fun shouldRestartApp() = isDashboardEnabled != interactor.isDashboardEnabled()
}