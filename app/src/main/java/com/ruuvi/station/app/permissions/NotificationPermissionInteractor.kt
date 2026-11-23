package com.ruuvi.station.app.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ruuvi.station.R
import com.ruuvi.station.util.ui.ToastDuration
import com.ruuvi.station.util.ui.showToast
import timber.log.Timber

class NotificationPermissionInteractor(val activity: AppCompatActivity): BasePermissionInteractor(activity) {

    override fun checkAndRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = permissionGranted(notificationPermission)
            Timber.d("permissionGranted = $permissionGranted")
            val shouldShowRationale = shouldShowRationale(notificationPermission)
            Timber.d("shouldShowRationale = $shouldShowRationale")

            if (!permissionGranted) {
                requestPermission(notificationPermission)
            }
        }
    }

    override fun onPermissionGranted() {
        // no actions needed
    }

    override fun onPermissionDenied() {
        showPermissionToast(activity.getString(R.string.notification_permission_needed))
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

    fun showPermissionToast(message: String) {
        activity.showToast(message, ToastDuration.Long)
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