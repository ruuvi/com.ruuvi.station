package com.ruuvi.station.onboarding.di

import com.ruuvi.station.onboarding.ui.OnboardingViewModel
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.provider

object OnboardingInjectionModule {
    val module = DI.Module(OnboardingInjectionModule.javaClass.name) {
        bind<OnboardingViewModel>() with provider { OnboardingViewModel(instance(), instance(), instance()) }
    }
}