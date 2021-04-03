package com.ruuvi.station.util.extensions

import android.content.Context
import android.content.pm.PackageManager
import timber.log.Timber

fun Context.isFirstInstall(): Boolean {
    try {
        val firstInstall = packageManager.getPackageInfo(packageName,0).firstInstallTime
        val lastUpdate = packageManager.getPackageInfo(packageName,0).lastUpdateTime
        return firstInstall == lastUpdate
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e)
        return true
    }
}

fun Context.isUpdateInstall(): Boolean {
    try {
        val firstInstall = packageManager.getPackageInfo(packageName,0).firstInstallTime
        val lastUpdate = packageManager.getPackageInfo(packageName,0).lastUpdateTime
        return firstInstall != lastUpdate
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e)
        return false
    }
}