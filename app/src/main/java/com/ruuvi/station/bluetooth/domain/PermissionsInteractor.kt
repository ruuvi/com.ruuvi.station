package com.ruuvi.station.bluetooth.domain

import android.Manifest
import android.Manifest.permission.*
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.R
import timber.log.Timber

class PermissionsInteractor(private val activity: Activity) {
    private val requiredPermissions = mutableListOf(
        ACCESS_COARSE_LOCATION,
        ACCESS_FINE_LOCATION
    ).also {
        if (BuildConfig.FILE_LOGS_ENABLED) it.add(WRITE_EXTERNAL_STORAGE)
    }

    fun arePermissionsGranted(): Boolean = getRequiredPermissions().isEmpty()

    fun requestPermissions(needBackground: Boolean) {
        val neededPermissions = getRequiredPermissions()
        if (neededPermissions.isNotEmpty()) {
            val alertDialog = AlertDialog.Builder(activity).create()
            alertDialog.setTitle(activity.getString(R.string.permission_dialog_title))
            alertDialog.setMessage(activity.getString(R.string.permission_dialog_request_message))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok)
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.setOnDismissListener { showPermissionDialog(neededPermissions) }
            alertDialog.show()
        } else if (enableBluetooth() && needBackground && backgroundLocationNeeded()) {
            requestBackgroundPermission()
        }
    }

    fun requestBackgroundPermission() {
        if (arePermissionsGranted() && backgroundLocationNeeded()) {
            val alertDialog = AlertDialog.Builder(activity).create()
            alertDialog.setTitle(activity.getString(R.string.permission_background_dialog_title))
            alertDialog.setMessage(activity.getString(R.string.permission_dialog_background_request_message))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, activity.getString(R.string.ok)
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.setOnDismissListener {
                ActivityCompat.requestPermissions(activity, arrayOf(ACCESS_BACKGROUND_LOCATION), REQUEST_CODE_PERMISSIONS)
            }
            alertDialog.show()
        }
    }

    private fun backgroundLocationNeeded() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun showPermissionDialog(requiredPermissions: List<String>): Boolean {
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, requiredPermissions.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
        return requiredPermissions.isNotEmpty()
    }

    private fun getRequiredPermissions(): List<String> {
        return requiredPermissions.mapNotNull { permission ->
            if (!isPermissionGranted(permission)) {
                Timber.d("$permission required")
                permission
            } else {
                null
            }
        }
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }

    fun showPermissionSnackbar() {
        val snackbar = Snackbar.make(activity.findViewById(android.R.id.content), activity.getString(R.string.permission_location_needed), Snackbar.LENGTH_LONG)
        snackbar.setAction(activity.getString(R.string.settings)) {
            val intent = Intent()
            val uri = Uri.fromParts("package", activity.packageName, null)
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = uri
            activity.startActivity(intent)
        }
        snackbar.show()
    }

    private fun enableBluetooth(): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        activity.startActivityForResult(enableBtIntent, REQUEST_CODE_BLUETOOTH)
        return false
    }

    private fun isPermissionGranted(permission: String) = ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        const val REQUEST_CODE_BLUETOOTH = 87
    }
}