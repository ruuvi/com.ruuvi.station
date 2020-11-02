package com.ruuvi.station.network.di

import com.ruuvi.station.network.domain.*
import com.ruuvi.station.network.ui.EmailEnterViewModel
import com.ruuvi.station.network.ui.EnterCodeViewModel
import com.ruuvi.station.network.ui.SignOutViewModel
import com.ruuvi.station.network.ui.SignedInViewModel
import kotlinx.coroutines.Dispatchers
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

object NetworkInjectionModule {
    val module = Kodein.Module(NetworkInjectionModule.javaClass.name) {
        bind<RuuviNetworkRepository>() with singleton { RuuviNetworkRepository(Dispatchers.IO) }

        bind<NetworkTokenRepository>() with singleton { NetworkTokenRepository(instance()) }

        bind<RuuviNetworkInteractor>() with singleton { RuuviNetworkInteractor(instance(), instance()) }

        bind<NetworkSignInInteractor>() with singleton { NetworkSignInInteractor(instance(), instance(), instance()) }

        bind<NetworkDataSyncInteractor>() with singleton {
            NetworkDataSyncInteractor(instance(), instance(), instance())
        }

        bind<EmailEnterViewModel>() with provider {
            EmailEnterViewModel(instance())
        }

        bind<EnterCodeViewModel>() with provider {
            EnterCodeViewModel(instance(), instance())
        }

        bind<SignedInViewModel>() with provider {
            SignedInViewModel(instance(), instance(), instance())
        }

        bind<SignOutViewModel>() with provider {
            SignOutViewModel(instance())
        }
    }
}