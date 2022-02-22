package com.ruuvi.station.dataforwarding.domain

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import timber.log.Timber

class LocationPermissionsInteractor(private val activity: Activity) {

    private val requiredLocationPermissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    fun requestLocationPermissionApi31(
        resultLauncher: ActivityResultLauncher<Array<String>>,
        requestBackgroundLocationPermission: ActivityResultLauncher<String>
    ) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
            val neededPermissions = getRequiredPermissions(requiredLocationPermissions)
            if (neededPermissions.size > 1) {
                val alertDialog = AlertDialog.Builder(activity).create()
                alertDialog.setMessage(activity.getString(R.string.data_forwarding_background_location_message_first))
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok)
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.setOnDismissListener {
                    resultLauncher.launch(neededPermissions.toTypedArray())
                }
                alertDialog.show()
            } else {
                requestBackgroundLocationPermissionApi31(requestBackgroundLocationPermission)
            }
        }
    }

    fun requestBackgroundLocationPermissionApi31(requestBackgroundLocationPermission: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R &&
            ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )) {
            val alertDialog = AlertDialog.Builder(activity).create()
            alertDialog.setTitle(activity.getString(R.string.permission_background_dialog_title))
            alertDialog.setMessage(activity.getString(R.string.data_forwarding_background_location_message_second))
            alertDialog.setButton(
                AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok)
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.setOnDismissListener {
                requestBackgroundLocationPermission.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            alertDialog.show()
        }
    }

    fun showLocationSnackbar() {
        val messageText = activity.getString(R.string.data_forwarding_background_location_message_bloked)
        val snackBar = Snackbar.make(activity.findViewById(android.R.id.content), messageText, Snackbar.LENGTH_LONG)
        snackBar.setAction(activity.getString(R.string.settings)) {
            val intent = Intent()
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = uri
            activity.startActivity(intent)
        }
        snackBar.show()
    }

    private fun getRequiredPermissions(permissions: List<String>): List<String> {
        return permissions.mapNotNull { permission ->
            if (!isPermissionGranted(permission)) {
                Timber.d("$permission required")
                permission
            } else {
                null
            }
        }
    }

    private fun isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
}