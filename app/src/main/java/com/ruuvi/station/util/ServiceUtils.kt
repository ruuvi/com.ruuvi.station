package com.ruuvi.station.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build

class ServiceUtils(val context: Context) {
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