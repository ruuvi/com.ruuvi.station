package com.ruuvi.station.dfu.di

import com.ruuvi.station.dfu.domain.FirmwareRepository
import com.ruuvi.station.dfu.domain.GitHubRepository
import com.ruuvi.station.dfu.domain.LatestFwInteractor
import com.ruuvi.station.dfu.ui.DfuAirUpdateViewModel
import com.ruuvi.station.dfu.ui.DfuUpdateViewModel
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import kotlinx.coroutines.Dispatchers
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.singleton

object DfuInjectionModule {
    val module = DI.Module(DfuInjectionModule.javaClass.name) {
        bind<DfuUpdateViewModel>() with factory { sensorId: TagSettingsViewModelArgs ->
            DfuUpdateViewModel(sensorId.tagId, instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<DfuAirUpdateViewModel>() with factory { sensorId: String ->
            DfuAirUpdateViewModel(sensorId, instance(), instance(), instance(), instance(), instance())
        }

        bind<GitHubRepository>() with singleton {
            GitHubRepository(Dispatchers.IO)
        }

        bind<LatestFwInteractor>() with singleton {
            LatestFwInteractor(instance())
        }

        bind<FirmwareRepository>() with singleton {
            FirmwareRepository(instance())
        }
    }
}