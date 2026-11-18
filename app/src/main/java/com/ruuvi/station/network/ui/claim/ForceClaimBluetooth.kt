package com.ruuvi.station.network.ui.claim

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.os.Build

internal object ForceClaimBluetooth {

    enum class Step { REQUEST_PERMISSIONS, BLUETOOTH_UNAVAILABLE, ENABLE_BLUETOOTH, START_READ }

    fun requiredPermissions(sdkInt: Int = Build.VERSION.SDK_INT): Array<String> =
        if (sdkInt >= Build.VERSION_CODES.S) {
            arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT)
        } else {
            arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
        }

    fun nextStep(permissionsGranted: Boolean, adapterAvailable: Boolean, adapterEnabled: Boolean): Step =
        when {
            !permissionsGranted -> Step.REQUEST_PERMISSIONS
            !adapterAvailable -> Step.BLUETOOTH_UNAVAILABLE
            !adapterEnabled -> Step.ENABLE_BLUETOOTH
            else -> Step.START_READ
        }
}
