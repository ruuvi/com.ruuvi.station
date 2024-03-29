package com.ruuvi.station.network.di

import com.ruuvi.station.network.domain.*
import com.ruuvi.station.network.ui.*
import com.ruuvi.station.network.ui.claim.ClaimSensorViewModel
import kotlinx.coroutines.Dispatchers
import org.kodein.di.Kodein
import org.kodein.di.generic.*

object NetworkInjectionModule {
    val module = Kodein.Module(NetworkInjectionModule.javaClass.name) {
        bind<RuuviNetworkRepository>() with singleton { RuuviNetworkRepository(Dispatchers.IO, instance(), instance()) }

        bind<SensorClaimInteractor>() with singleton { SensorClaimInteractor(instance(), instance(), instance(), instance(), instance(), instance()) }

        bind<NetworkTokenRepository>() with singleton { NetworkTokenRepository(instance()) }

        bind<RuuviNetworkInteractor>() with singleton { RuuviNetworkInteractor(instance(), instance(), instance(), instance(), instance(), instance()) }

        bind<NetworkSignInInteractor>() with singleton { NetworkSignInInteractor(instance(), instance(), instance(), instance(), instance(), instance()) }

        bind<NetworkRequestExecutor.NetworkJobManager>() with singleton { NetworkRequestExecutor.NetworkJobManager() }

        bind<NetworkRequestExecutor>() with singleton { NetworkRequestExecutor(instance(), instance(), instance(), instance(), instance()) }

        bind<NetworkDataSyncInteractor>() with singleton {
            NetworkDataSyncInteractor(instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<NetworkShareListInteractor>() with singleton { NetworkShareListInteractor(instance()) }

        bind<NetworkApplicationSettings>() with  singleton {
            NetworkApplicationSettings(instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<NetworkAlertsSyncInteractor>() with  singleton {
            NetworkAlertsSyncInteractor(instance(), instance())
        }

        bind<SubscriptionInfoSyncInteractor>() with singleton {
            SubscriptionInfoSyncInteractor(instance(), instance())
        }

        bind<ShareSensorViewModel>() with factory { tagId: String ->
            ShareSensorViewModel(tagId, instance(), instance(), instance(), instance())
        }

        bind<ClaimSensorViewModel>() with factory { sensorId: String ->
            ClaimSensorViewModel(sensorId, instance(), instance(), instance(), instance())
        }

        bind<NetworkResponseLocalizer>() with provider { NetworkResponseLocalizer(instance()) }

        bind<MyAccountViewModel>() with provider {
            MyAccountViewModel(instance(), instance(), instance(), instance())
        }

        bind<SignInViewModel>() with provider {
            SignInViewModel(instance(), instance(), instance())
        }
    }
}