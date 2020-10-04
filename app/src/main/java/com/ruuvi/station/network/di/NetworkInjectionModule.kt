package com.ruuvi.station.network.di

import com.ruuvi.station.network.domain.RuuviNetworkRepository
import com.ruuvi.station.network.domain.NetworkTokenInteractor
import com.ruuvi.station.network.ui.EmailEnterViewModel
import com.ruuvi.station.network.ui.EnterCodeViewModel
import com.ruuvi.station.network.ui.SignOutViewModel
import com.ruuvi.station.network.ui.SignedInViewModel
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object NetworkInjectionModule {
    val module = Kodein.Module(NetworkInjectionModule.javaClass.name) {
        bind<RuuviNetworkRepository>() with singleton { RuuviNetworkRepository() }

        bind<NetworkTokenInteractor>() with singleton { NetworkTokenInteractor(instance()) }

        bind<EmailEnterViewModel>() with provider {
            EmailEnterViewModel(instance(), instance())
        }

        bind<EnterCodeViewModel>() with provider {
            EnterCodeViewModel(instance(), instance())
        }

        bind<SignedInViewModel>() with provider {
            SignedInViewModel(instance())
        }

        bind<SignOutViewModel>() with provider {
            SignOutViewModel(instance())
        }
    }
}