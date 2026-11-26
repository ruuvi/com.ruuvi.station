package com.ruuvi.station.app.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.ruuvi.station.R

/**
 * Explains why a permission is needed and offers a direct route to the app's system settings.
 * Once a permission is permanently denied, requesting it again cannot reopen the system dialog,
 * so the settings page is the only way for the user to grant it.
 */
fun Context.showPermissionSettingsDialog(@StringRes message: Int): AlertDialog =
    AlertDialog.Builder(this, R.style.CustomAlertDialog)
        .setMessage(message)
        .setNegativeButton(R.string.cancel, null)
        .setPositiveButton(R.string.open_settings) { _, _ -> openAppSettings() }
        .show()

fun Context.openAppSettings() {
    startActivity(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
    )
}
