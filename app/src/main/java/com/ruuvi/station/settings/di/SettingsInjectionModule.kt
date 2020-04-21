package com.ruuvi.station.settings.di

import android.arch.lifecycle.ViewModel
import com.ruuvi.station.settings.ui.AppSettingsBackgroundScanViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

object SettingsInjectionModule {
    val module = Kodein.Module(SettingsInjectionModule.javaClass.name) {
        bind<ViewModel>(tag = AppSettingsBackgroundScanViewModel::class.java.simpleName) with provider { AppSettingsBackgroundScanViewModel(instance()) }
    }
}