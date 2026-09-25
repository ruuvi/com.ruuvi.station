package com.ruuvi.station.firebase.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.Firebase
import com.ruuvi.station.firebase.domain.FirebaseInteractor
import com.ruuvi.station.firebase.domain.PushAlertInteractor
import com.ruuvi.station.firebase.domain.PushRegisterInteractor
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object FirebaseInjectionModule {
    val module = DI.Module(FirebaseInjectionModule.javaClass.name) {

        bind<FirebaseAnalytics>() with singleton { Firebase.analytics }

        bind<FirebaseInteractor>() with singleton {
            FirebaseInteractor(instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<PushAlertInteractor>() with singleton { PushAlertInteractor(instance(), instance(), instance()) }

        bind<PushRegisterInteractor>() with singleton { PushRegisterInteractor(instance(), instance(), instance()) }
    }
}