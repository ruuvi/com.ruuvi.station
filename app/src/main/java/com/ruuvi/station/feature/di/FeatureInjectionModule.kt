package com.ruuvi.station.feature.di

import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.feature.provider.FirebaseFeatureFlagProvider
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object FeatureInjectionModule {
    val module = DI.Module(FeatureInjectionModule.javaClass.name) {

        bind<RuntimeBehavior>() with singleton {
            RuntimeBehavior().also {
                it.addProvider(FirebaseFeatureFlagProvider())
                it.addProvider(instance())
            }
        }

        bind<RuntimeFeatureFlagProvider>() with singleton { RuntimeFeatureFlagProvider(instance()) }
    }
}