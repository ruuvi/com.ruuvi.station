package com.ruuvi.station.network.di

import com.ruuvi.station.network.domain.*
import com.ruuvi.station.network.ui.*
import kotlinx.coroutines.Dispatchers
import org.kodein.di.Kodein
import org.kodein.di.generic.*

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
            EnterCodeViewModel(instance(), instance(), instance())
        }

        bind<SignOutViewModel>() with provider {
            SignOutViewModel(instance())
        }

        bind<ShareSensorViewModel>() with factory { tagId: String ->
            ShareSensorViewModel(tagId, instance())
        }
    }
}