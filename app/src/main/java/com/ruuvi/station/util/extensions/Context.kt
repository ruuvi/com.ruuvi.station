package com.ruuvi.station.util.extensions

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.core.content.ContextCompat
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import timber.log.Timber

fun Context.sendFeedback(){
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")
    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf( "contact@ruuvi.com" ))
    intent.putExtra(Intent.EXTRA_SUBJECT, "Ruuvi Station Android Feedback")
    val body = """
                       
Device: ${Build.MANUFACTURER} ${Build.MODEL}
Android version: ${Build.VERSION.RELEASE}
App: ${applicationInfo.loadLabel(packageManager)} ${BuildConfig.VERSION_NAME}"""
    intent.putExtra(Intent.EXTRA_TEXT, body)

    startActivity(Intent.createChooser(intent, getString(R.string.send_email)))
}

fun Context.openUrl(url: String){
    val webIntent = Intent(Intent.ACTION_VIEW)
    webIntent.data = Uri.parse(url)
    startActivity(webIntent)
}

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

fun Context.isDarkMode(): Boolean {
    return if (getDefaultNightMode() == MODE_NIGHT_FOLLOW_SYSTEM)
        resources.configuration.uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
    else getDefaultNightMode() == MODE_NIGHT_YES
}