package com.ruuvi.station.bluetooth.di

import android.app.PendingIntent
import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.IRuuviTagScanner
import com.ruuvi.station.bluetooth.domain.*
import com.ruuvi.station.bluetooth.util.ScannerSettings
import com.ruuvi.station.startup.ui.StartupActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.util.TimeUtils
import com.ruuvi.station.util.test.FakeScanResultsSender
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

@ExperimentalCoroutinesApi
object BluetoothScannerInjectionModule {

    val module = Kodein.Module(BluetoothScannerInjectionModule.javaClass.name) {

        bind<BluetoothInteractor>() with singleton {
            BluetoothLibrary.getBluetoothInteractor(instance(), instance(), instance())
        }

        bind<BluetoothGattInteractor>() with singleton { BluetoothGattInteractor(instance(), instance(), instance(), instance(), instance()) }

        bind<BluetoothStateReceiver>() with singleton { BluetoothStateReceiver(instance(), instance()) }

        bind<IRuuviTagScanner.OnTagFoundListener>() with singleton { instance<DefaultOnTagFoundListener>() }

        bind<DefaultOnTagFoundListener>() with singleton {
            DefaultOnTagFoundListener(instance(), instance(), instance(), instance(), instance(), instance())
        }

        bind<FakeScanResultsSender>() with singleton { FakeScanResultsSender(instance()) }

        bind<ScannerSettings>() with singleton {
            object : ScannerSettings {
                var context = instance<Context>()
                var prefs = instance<Preferences>()

                override fun allowBackgroundScan(): Boolean {
                    return prefs.backgroundScanMode != BackgroundScanModes.DISABLED
                }

                override fun getBackgroundScanIntervalMilliseconds(): Long {
                    return prefs.backgroundScanInterval * 1000L
                }

                override fun getNotificationIconId() = R.drawable.ic_ruuvi_bgscan_icon

                override fun getNotificationTitle(): String {
                    val scanInterval = prefs.backgroundScanInterval
                    val stringMessage = context.getString(R.string.background_notification_scanning_every)
                    return "$stringMessage ${TimeUtils.convertSecondsToTextTemp(context, scanInterval)}"
                }

                override fun getNotificationText() = context.getString(R.string.background_notification_message)

                override fun getNotificationPendingIntent(): PendingIntent? {
                    val resultIntent = StartupActivity.createIntentForNotification(context)
                    return PendingIntent
                        .getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                }
            }
        }

        bind<SensorFwVersionInteractor>() with singleton { SensorFwVersionInteractor(instance()) }

        bind<BluetoothDevicesInteractor>() with singleton { BluetoothDevicesInteractor(instance()) }

        bind<DfuInteractor>() with singleton { DfuInteractor(instance()) }
    }
}