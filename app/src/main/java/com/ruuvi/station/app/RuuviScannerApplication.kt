package com.ruuvi.station.app

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.IntentFilter
import android.os.PowerManager
import androidx.appcompat.app.AppCompatDelegate
import com.raizlabs.android.dbflow.config.FlowManager
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.app.di.AppInjectionModules
import com.ruuvi.station.app.domain.migration.ImageMigrationInteractor
import com.ruuvi.station.app.domain.migration.Version3MigrationInteractor
import com.ruuvi.station.app.domain.migration.VisibleMeasurementsMigrationInteractor
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.domain.BluetoothStateReceiver
import com.ruuvi.station.feature.domain.RuntimeBehavior
import com.ruuvi.station.feature.provider.RuntimeFeatureFlagProvider
import com.ruuvi.station.network.domain.NetworkDataSyncInteractor
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.util.Foreground
import com.ruuvi.station.util.ForegroundListener
import com.ruuvi.station.util.ReleaseTree
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.conf.ConfigurableKodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import timber.log.Timber

class RuuviScannerApplication : Application(), KodeinAware {
    override val kodein = ConfigurableKodein()

    val defaultOnTagFoundListener: DefaultOnTagFoundListener by instance()
    private val bluetoothReceiver: BluetoothStateReceiver by instance()
    private val foreground: Foreground by instance()
    private val networkInteractor: RuuviNetworkInteractor by instance()
    private val networkDataSyncInteractor: NetworkDataSyncInteractor by instance()
    private val preferencesRepository: PreferencesRepository by instance()
    private val runtimeFeatureFlagProvider: RuntimeFeatureFlagProvider by instance()
    private val runtimeBehavior: RuntimeBehavior by instance()
    private val imageMigrationInteractor: ImageMigrationInteractor by instance()
    private val version3MigrationInteractor: Version3MigrationInteractor by instance()
    private val visibleMeasurementsMigrationInteractor: VisibleMeasurementsMigrationInteractor by instance()


    private var isInForeground: Boolean = false

    private val listener: ForegroundListener = object : ForegroundListener {
        override fun onBecameForeground() {
            Timber.d("onBecameForeground")
            isInForeground = true
            defaultOnTagFoundListener.isForeground = true
            networkDataSyncInteractor.startAutoRefresh()
            runtimeBehavior.refreshFeatureFlags()
        }

        override fun onBecameBackground() {
            Timber.d("onBecameBackground")
            isInForeground = false
            defaultOnTagFoundListener.isForeground = false
            networkDataSyncInteractor.stopAutoRefresh()
        }
    }

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.FILE_LOGS_ENABLED) {
            Timber.plant(ReleaseTree())
        } else if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        setupDependencyInjection()

        FlowManager.init(this)

        imageMigrationInteractor.migrateDefaultImages()
        version3MigrationInteractor.migrate()
        visibleMeasurementsMigrationInteractor.migrate()

        registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))

        defaultOnTagFoundListener.isForeground = isInForeground
        foreground.addListener(listener)

        applyDarkModeSettings()
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

    private fun applyDarkModeSettings() {
        val mode = preferencesRepository.getDarkMode()
        AppCompatDelegate.setDefaultNightMode(mode.code)
    }
}