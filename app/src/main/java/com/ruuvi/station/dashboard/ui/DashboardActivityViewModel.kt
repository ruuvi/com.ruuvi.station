package com.ruuvi.station.dashboard.ui

import androidx.lifecycle.*
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.NetworkTokenRepository
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class DashboardActivityViewModel(
    private val tagInteractor: TagInteractor,
    val converter: UnitsConverter,
    private val networkDataSyncInteractor: NetworkDataSyncInteractor,
    val preferencesRepository: PreferencesRepository,
    private val tokenRepository: NetworkTokenRepository
) : ViewModel() {

    val tagsFlow: Flow<List<RuuviTag>> = flow {
        while (true) {
            emit(tagInteractor.getTags())
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    val syncEvents = networkDataSyncInteractor.syncEvents

    val userEmail = preferencesRepository.getUserEmailLiveData()

    fun signOut() {
        networkDataSyncInteractor.stopSync()
        tokenRepository.signOut { }
    }
}