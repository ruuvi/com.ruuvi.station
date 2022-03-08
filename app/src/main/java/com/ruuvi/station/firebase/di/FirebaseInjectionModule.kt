package com.ruuvi.station.firebase.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object FirebaseInjectionModule {
    val module = Kodein.Module(FirebaseInjectionModule.javaClass.name) {

        bind<FirebaseAnalytics>() with singleton { Firebase.analytics }

        bind<FirebaseInteractor>() with singleton {
            FirebaseInteractor(instance(), instance(), instance(), instance(), instance())
        }
    }
}