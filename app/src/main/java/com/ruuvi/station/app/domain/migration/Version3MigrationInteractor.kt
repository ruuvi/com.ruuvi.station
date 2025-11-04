package com.ruuvi.station.app.domain.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.dashboard.DashboardTapAction
import com.ruuvi.station.network.domain.NetworkApplicationSettings

class Version3MigrationInteractor(
    val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val networkApplicationSettings: NetworkApplicationSettings,
) {
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private var migrationDone: Boolean
        get() = preferences.getBoolean(MIGRATION_DONE_KEY, false)
        set(locationEnabled) {
            preferences.edit { putBoolean(MIGRATION_DONE_KEY, locationEnabled) }
        }

    private fun isVersionSuitable(): Boolean {
        return BuildConfig.VERSION_CODE in (MIN_VERSION..MAX_VERSION)
    }

    fun migrate() {
        if (isVersionSuitable() && !migrationDone) {
            migrationDone = true
            preferencesRepository.updateDashboardTapAction(DashboardTapAction.OPEN_CARD)
            networkApplicationSettings.updateDashboardTapAction()
        }
    }

    companion object {
        private const val MIN_VERSION = 216131
        private const val MAX_VERSION = 300099
        private const val PREFERENCES_NAME = "migration3"
        private const val MIGRATION_DONE_KEY = "migration_done"
    }
}