package com.ruuvi.station.onboarding.di

import com.ruuvi.station.onboarding.ui.OnboardingViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider

object OnboardingInjectionModule {
    val module = Kodein.Module(OnboardingInjectionModule.javaClass.name) {
        bind<OnboardingViewModel>() with provider { OnboardingViewModel(instance()) }
    }
}