package com.ruuvi.station.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.R
import timber.log.Timber
import java.util.*

class PermissionsHelper(private val activity: AppCompatActivity) {

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
        getNeededPermissions().isEmpty()

    fun requestPermissions() {
        val neededPermissions = getNeededPermissions()
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
        activity.startActivityForResult(enableBtIntent, 87)
        return false
    }

    private fun getNeededPermissions(): List<String> {
        val permissionCoarseLocation = ContextCompat.checkSelfPermission(activity,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        val listPermissionsNeeded = ArrayList<String>()

        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            Timber.d("ACCESS_COARSE_LOCATION needed")
        }

        val permissionWriteStorage = ContextCompat.checkSelfPermission(activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            Timber.d("WRITE_EXTERNAL_STORAGE needed")
        }

        val permissionFineLocation = ContextCompat.checkSelfPermission(activity,
            Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            Timber.d("ACCESS_FINE_LOCATION needed")
        }

        return listPermissionsNeeded
    }

    private fun showPermissionDialog(activity: AppCompatActivity): Boolean {
        val listPermissionsNeeded = getNeededPermissions()

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, listPermissionsNeeded.toTypedArray(), 10)
        }

        return listPermissionsNeeded.isNotEmpty()
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
}