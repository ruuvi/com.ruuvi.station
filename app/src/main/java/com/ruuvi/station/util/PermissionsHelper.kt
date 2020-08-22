package com.ruuvi.station.util

import android.Manifest
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ruuvi.station.R
import timber.log.Timber

class PermissionsHelper(private val activity: AppCompatActivity) {
    private val requiredPermissions = listOf(
        ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    fun checkIsLocationEnabled() {
        if (!isLocationEnabled()) {
            val builder = AlertDialog.Builder(activity)
            builder.setTitle(activity.getString(R.string.locationServices))
            builder.setMessage(activity.getString(R.string.enableLocationServices))
            builder.setPositiveButton(android.R.string.ok) { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                activity.startActivity(intent)
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
            builder.show()
        }
    }

    fun arePermissionsGranted(): Boolean =
        getRequiredPermissions().isEmpty()

    fun requestPermissions() {
        val neededPermissions = getRequiredPermissions()
        if (neededPermissions.isNotEmpty()) {
            if (neededPermissions.size == 1 && neededPermissions.first() == Manifest.permission.ACCESS_FINE_LOCATION) {
                showPermissionDialog(activity)
            } else {
                val alertDialog = AlertDialog.Builder(activity).create()
                alertDialog.setTitle(activity.getString(R.string.permission_dialog_title))
                alertDialog.setMessage(activity.getString(R.string.permission_dialog_request_message))
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok)
                ) { dialog, _ -> dialog.dismiss() }
                alertDialog.setOnDismissListener { showPermissionDialog(activity) }
                alertDialog.show()
            }
        } else {
            requestBluetoothPermissions()
        }
    }

    fun requestBluetoothPermissions(): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH)
        return false
    }

    private fun getRequiredPermissions(): List<String> {
        return requiredPermissions.mapNotNull { permission ->
            val isPermissionGranted = ContextCompat.checkSelfPermission(activity, permission) == PERMISSION_GRANTED

            if (!isPermissionGranted) {
                Timber.d("$permission required")
                permission
            } else {
                null
            }
        }
    }

    private fun showPermissionDialog(activity: AppCompatActivity): Boolean {
        val requiredPermissions = getRequiredPermissions()

        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, requiredPermissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }

        return requiredPermissions.isNotEmpty()
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    private fun isLocationEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        } else {
            try {
                val locationMode = Settings.Secure.getInt(activity.contentResolver, Settings.Secure.LOCATION_MODE)
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                Timber.e(e, "Could not get LOCATION_MODE")
                false
            }
        }

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        const val REQUEST_CODE_BLUETOOTH = 87
    }
}