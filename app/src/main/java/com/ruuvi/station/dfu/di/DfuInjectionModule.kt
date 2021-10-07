package com.ruuvi.station.dfu.di

import com.ruuvi.station.dfu.ui.DfuUpdateViewModel
import com.ruuvi.station.tagsettings.di.TagSettingsViewModelArgs
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance

object DfuInjectionModule {
    val module = Kodein.Module(DfuInjectionModule.javaClass.name) {
        bind<DfuUpdateViewModel>() with factory { sensorId: TagSettingsViewModelArgs ->
            DfuUpdateViewModel(sensorId.tagId, instance())
        }
    }
}