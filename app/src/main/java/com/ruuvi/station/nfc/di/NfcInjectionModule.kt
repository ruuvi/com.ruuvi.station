package com.ruuvi.station.nfc.di

import com.ruuvi.station.nfc.domain.NfcResultInteractor
import org.kodein.di.DI
import org.kodein.di.bind
import org.kodein.di.instance
import org.kodein.di.singleton

object NfcInjectionModule {
    val module = DI.Module(NfcInjectionModule.javaClass.name) {

        bind<NfcResultInteractor>() with singleton { NfcResultInteractor(instance(), instance(), instance()) }
    }
}