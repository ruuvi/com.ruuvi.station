package com.ruuvi.station.app.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.R
import timber.log.Timber

class NotificationPermissionInteractor(val activity: AppCompatActivity): BasePermissionInteractor(activity) {

    override fun checkAndRequest() {
        val permissionGranted = permissionGranted(notificationPermission)
        Timber.d("permissionGranted = $permissionGranted")
        val shouldShowRationale = shouldShowRationale(notificationPermission)
        Timber.d("shouldShowRationale = $shouldShowRationale")

        if (!permissionGranted) {
            requestPermission(notificationPermission)
        }
    }

    override fun onPermissionGranted() {
        // no actions needed
    }

    override fun onPermissionDenied() {
        showPermissionSnackbar("To enable alerts please allow Notifications in Settings")
    }

    companion object {
        const val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
    }
}

abstract class BasePermissionInteractor(private val activity: AppCompatActivity) {
    abstract fun checkAndRequest()
    abstract fun onPermissionGranted()
    abstract fun onPermissionDenied()

    protected fun permissionGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    protected fun shouldShowRationale(permission: String): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity,
            permission
        )

    protected fun requestPermission(permission: String) {
        Timber.d("Request permission $permission")
        requestPermissionLauncher.launch(permission)
    }

    fun showPermissionSnackbar(message: String) {
        val snackBar = Snackbar.make(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        snackBar.setAction(activity.getString(R.string.settings)) {
            val intent = Intent()
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = uri
            activity.startActivity(intent)
        }
        snackBar.show()
    }

    private val requestPermissionLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Timber.d("Permission granted")
                onPermissionGranted()
            } else {
                Timber.d("Permission denied")
                onPermissionDenied()
            }
        }
}