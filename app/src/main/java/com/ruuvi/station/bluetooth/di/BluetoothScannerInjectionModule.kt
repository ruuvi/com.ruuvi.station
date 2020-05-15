package com.ruuvi.station.bluetooth.di

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.IRuuviTagScanner
import com.ruuvi.station.bluetooth.util.ScannerSettings
import com.ruuvi.station.feature.StartupActivity
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.bluetooth.domain.BluetoothStateWatcher
import com.ruuvi.station.util.TimeUtils
import com.ruuvi.station.util.test.FakeScanResultsSender
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton

object BluetoothScannerInjectionModule {

    val module = Kodein.Module(BluetoothScannerInjectionModule.javaClass.name) {

        bind<BluetoothInteractor>() with singleton { BluetoothLibrary.getBluetoothInteractor(instance(), instance(), instance()) }

        bind<BluetoothStateWatcher>() with singleton { BluetoothStateWatcher(instance()) }

        bind<IRuuviTagScanner.OnTagFoundListener>() with singleton { instance<DefaultOnTagFoundListener>() }

        bind<DefaultOnTagFoundListener>() with singleton { DefaultOnTagFoundListener(instance(), instance(), instance()) }

        bind<FakeScanResultsSender>() with singleton { FakeScanResultsSender(instance()) }

        bind<ScannerSettings>() with singleton {
            object : ScannerSettings {
                var context = instance<Context>()
                var prefs = Preferences(context)

                override fun allowBackgroundScan(): Boolean {
                    return prefs.backgroundScanMode != BackgroundScanModes.DISABLED
                }

                override fun getBackgroundScanInterval(): Long {
                    return prefs.backgroundScanInterval * 1000L
                }

                override fun getNotificationIconId() = R.drawable.ic_ruuvi_bgscan_icon

                override fun getNotificationTitle(): String {
                    val seconds = prefs.backgroundScanInterval
                    val stringMessage = context.getString(R.string.scanner_notification_scanning_every)
                    return "$stringMessage ${TimeUtils.convertSecondsToText(context, seconds)}"
                }

                override fun getNotificationText() = context.getString(R.string.scanner_notification_message)

                override fun getNotificationPendingIntent(): PendingIntent? {
                    val resultIntent = Intent(context, StartupActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    return PendingIntent.getActivity(
                            context,
                            0,
                            resultIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    )
                }
            }
        }
    }
}