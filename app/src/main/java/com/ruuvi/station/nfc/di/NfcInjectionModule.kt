package com.ruuvi.station.nfc.di

import com.ruuvi.station.nfc.domain.NfcResultInteractor
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object NfcInjectionModule {
    val module = Kodein.Module(NfcInjectionModule.javaClass.name) {

        bind<NfcResultInteractor>() with singleton { NfcResultInteractor(instance()) }
    }
}