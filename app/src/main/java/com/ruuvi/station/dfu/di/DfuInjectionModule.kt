package com.ruuvi.station.dfu.di

import com.ruuvi.station.dfu.domain.GitHubRepository
import com.ruuvi.station.dfu.domain.LatestFwInteractor
import com.ruuvi.station.dfu.ui.DfuUpdateViewModel
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import kotlinx.coroutines.Dispatchers
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object DfuInjectionModule {
    val module = Kodein.Module(DfuInjectionModule.javaClass.name) {
        bind<DfuUpdateViewModel>() with factory { sensorId: TagSettingsViewModelArgs ->
            DfuUpdateViewModel(sensorId.tagId, instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<GitHubRepository>() with singleton {
            GitHubRepository(Dispatchers.IO)
        }

        bind<LatestFwInteractor>() with singleton {
            LatestFwInteractor(instance())
        }
    }
}