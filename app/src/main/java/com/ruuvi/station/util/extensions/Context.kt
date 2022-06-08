package com.ruuvi.station.util.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
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

@ColorInt
fun Context.resolveColorAttr(@AttrRes colorAttr: Int): Int {
    val resolvedAttr = resolveThemeAttr(colorAttr)
    // resourceId is used if it's a ColorStateList, and data if it's a color reference or a hex color
    val colorRes = if (resolvedAttr.resourceId != 0) resolvedAttr.resourceId else resolvedAttr.data
    return ContextCompat.getColor(this, colorRes)
}

fun Context.resolveThemeAttr(@AttrRes attrRes: Int): TypedValue {
    val typedValue = TypedValue()
    theme.resolveAttribute(attrRes, typedValue, true)
    return typedValue
}