package com.ruuvi.station.feature.di

import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.feature.provider.FirebaseFeatureFlagProvider
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object FeatureInjectionModule {
    val module = Kodein.Module(FeatureInjectionModule.javaClass.name) {

        bind<RuntimeBehavior>() with singleton {
            RuntimeBehavior().also {
                it.addProvider(FirebaseFeatureFlagProvider())
                it.addProvider(instance())
            }
        }

        bind<RuntimeFeatureFlagProvider>() with singleton { RuntimeFeatureFlagProvider(instance()) }
    }
}