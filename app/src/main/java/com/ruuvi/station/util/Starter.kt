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

class Starter(val that: AppCompatActivity) {
    fun startScanning(): Boolean {
        if (!isLocationEnabled()) {
            val builder = AlertDialog.Builder(that)
            builder.setTitle(that.getString(R.string.locationServices))
            builder.setMessage(that.getString(R.string.enableLocationServices))
            builder.setPositiveButton(android.R.string.ok) { _, i ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                that.startActivity(intent)
            }
            builder.setNegativeButton(android.R.string.cancel) { _, i -> }
            builder.show()
            return false
        }
        return true
    }

    fun getThingsStarted() {
        requestPermissions()
    }

    fun getNeededPermissions(): List<String> {
        val permissionCoarseLocation = ContextCompat.checkSelfPermission(that,
            Manifest.permission.ACCESS_COARSE_LOCATION)

        val listPermissionsNeeded = ArrayList<String>()

        if (permissionCoarseLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        val permissionWriteStorage = ContextCompat.checkSelfPermission(that,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissionWriteStorage != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val permissionFineLocation = ContextCompat.checkSelfPermission(that,
            Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionFineLocation != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val permissionBackgroundLocation = ContextCompat.checkSelfPermission(that,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION)

            if (permissionBackgroundLocation != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
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

    fun requestPermissions() {
        if (getNeededPermissions().isNotEmpty()) {
            val alertDialog = AlertDialog.Builder(that).create()
            alertDialog.setTitle(that.getString(R.string.permission_dialog_title))
            alertDialog.setMessage(that.getString(R.string.permission_dialog_request_message))
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, that.getString(R.string.ok)
            ) { dialog, _ -> dialog.dismiss() }
            alertDialog.setOnDismissListener { showPermissionDialog(that) }
            alertDialog.show()
        } else {
            checkBluetooth()
        }
    }

    fun checkBluetooth(): Boolean {
        if (isBluetoothEnabled()) {
            return true
        }
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        that.startActivityForResult(enableBtIntent, 87)
        return false
    }

    private fun isLocationEnabled(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val locationManager = that.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locationManager.isLocationEnabled
        } else {
            try {
                val locationMode = Settings.Secure.getInt(that.contentResolver, Settings.Secure.LOCATION_MODE)
                locationMode != Settings.Secure.LOCATION_MODE_OFF
            } catch (e: Settings.SettingNotFoundException) {
                Timber.e(e, "Could not get LOCATION_MODE")
                false
            }
        }

    fun isBluetoothEnabled(): Boolean {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
}