package com.ruuvi.station.startup.domain

import com.ruuvi.station.app.preferences.PreferencesRepository

class StartupActivityInteractor(private val preferencesRepository: PreferencesRepository) {

    fun isFirstStart(): Boolean = preferencesRepository.isFirstStart()
}