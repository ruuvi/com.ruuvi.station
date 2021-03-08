package com.ruuvi.station.app

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.os.PowerManager
import com.facebook.stetho.Stetho
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.di.AppInjectionModules
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.domain.BluetoothStateReceiver
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.ForegroundListener
import com.ruuvi.station.util.ReleaseTree
import com.ruuvi.station.util.extensions.diffGreaterThan
import com.ruuvi.station.util.test.FakeScanResultsSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import timber.log.Timber
import java.util.*

@ExperimentalCoroutinesApi
class RuuviScannerApplication : Application(), KodeinAware {
    override val kodein = ConfigurableKodein()

    val defaultOnTagFoundListener: DefaultOnTagFoundListener by instance()
    private val fakesSender: FakeScanResultsSender by instance()
    private val bluetoothReceiver: BluetoothStateReceiver by instance()
    private val foreground: Foreground by instance()
    private val networkInteractor: RuuviNetworkInteractor by instance()
    private val networkDataSyncInteractor: NetworkDataSyncInteractor by instance()
    private val preferencesRepository: PreferencesRepository by instance()

    private var isInForeground: Boolean = false

    private val listener: ForegroundListener = object : ForegroundListener {
        override fun onBecameForeground() {
            isInForeground = true
            defaultOnTagFoundListener.isForeground = true
            updateNetwork()
        }

        override fun onBecameBackground() {
            isInForeground = false
            defaultOnTagFoundListener.isForeground = false
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else if (BuildConfig.FILE_LOGS_ENABLED) {
            Timber.plant(ReleaseTree())
        }

        setupDependencyInjection()

        //Lingver.init(this, Locale(preferencesRepository.getLocale()))

        FlowManager.init(this)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
            //turn on for debug if you don't have real ruuvi tag
            //fakesSender.startSendFakes()
        }

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        defaultOnTagFoundListener.isForeground = isInForeground
        foreground.addListener(listener)

        updateNetwork()
    }

    private fun setupDependencyInjection() {
        kodein.apply {
            addImport(AppInjectionModules.module)

            addImport(Kodein.Module(javaClass.name) {
                bind<Application>() with singleton { this@RuuviScannerApplication }
                bind<PowerManager>() with singleton {
                    this@RuuviScannerApplication.getSystemService(Context.POWER_SERVICE) as PowerManager
                }
            })
        }
    }

    private fun updateNetwork() {
        if (networkInteractor.signedIn && Date(preferencesRepository.getLastSyncDate()).diffGreaterThan(60*1000)) {
            networkDataSyncInteractor.syncNetworkData()
        }
    }

    companion object {
        private const val isBluetoothFakingEnabledInDebug = false

        val isBluetoothFakingEnabled =
            if (BuildConfig.DEBUG) isBluetoothFakingEnabledInDebug
            else /*if release */ false
    }
}