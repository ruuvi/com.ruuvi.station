package com.ruuvi.station.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ruuvi.station.service.AltBeaconScannerForegroundService

class ServiceUtils(val context: Context) {

    fun stopForegroundService(): ServiceUtils {
        val scannerService = Intent(context, AltBeaconScannerForegroundService::class.java)
        context.stopService(scannerService)
        return this
    }

    fun startForegroundService(): ServiceUtils {
        if (!isRunning(AltBeaconScannerForegroundService::class.java)) {
            val scannerService = Intent(context, AltBeaconScannerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(scannerService)
            } else {
                context.startService(scannerService)
            }
        }
        return this
    }

    fun forceStartIfRunningForegroundService(): ServiceUtils {
        if (isRunning(AltBeaconScannerForegroundService::class.java)) {
            val scannerService = Intent(context, AltBeaconScannerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(scannerService)
            } else {
                context.startService(scannerService)
            }
        }
        return this
    }

    fun isRunning(serviceClass: Class<*>): Boolean {
        val mgr = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        for (service in mgr!!.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}