package com.ruuvi.station.tagsettings.di

import com.ruuvi.station.tagsettings.domain.HumidityCalibrationInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.tagsettings.ui.TagSettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

@ExperimentalCoroutinesApi
object TagSettingsInjectionModule {
    val module = Kodein.Module(TagSettingsInjectionModule.javaClass.name) {

        bind<TagSettingsInteractor>() with singleton {
            TagSettingsInteractor(instance(), instance())
        }

        bind<TagSettingsViewModel>() with factory { args: TagSettingsViewModelArgs ->
            TagSettingsViewModel(args.tagId, instance(), instance())
        }

        bind<HumidityCalibrationInteractor>() with singleton {
            HumidityCalibrationInteractor(instance())
        }
    }
}