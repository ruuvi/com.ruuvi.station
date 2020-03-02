package com.ruuvi.station.app

import android.app.Application
import com.facebook.stetho.Stetho
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.di.AppInjectionModules
import com.ruuvi.station.bluetooth.domain.BluetoothScannerInteractor
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.singleton
import timber.log.Timber

class RuuviScannerApplication() : Application(), KodeinAware
{
    val bluetoothScannerInteractor = BluetoothScannerInteractor(this)

    override val kodein = ConfigurableKodein()

    override fun onCreate() {
        super.onCreate()

        if(BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())

        setupDependencyInjection()

        FlowManager.init(this)

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this)
        }
    }

    private fun setupDependencyInjection() {
        kodein.apply {
            addImport(AppInjectionModules.module)

            addImport(Kodein.Module(javaClass.name) {
                bind<Application>() with singleton { this@RuuviScannerApplication }
            })
        }
    }

    companion object {
        private const val isBluetoothFakingEnabledInDebug = false

        val isBluetoothFakingEnabled =
            if (BuildConfig.DEBUG) isBluetoothFakingEnabledInDebug
            else /*if release */ false
    }
}