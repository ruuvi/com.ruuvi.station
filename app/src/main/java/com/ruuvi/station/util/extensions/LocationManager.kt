package com.ruuvi.station.util.extensions

import android.location.LocationManager
import android.os.Build

fun LocationManager.locationEnabled(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    this.isLocationEnabled
} else {
    this.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
            this.isProviderEnabled(LocationManager.GPS_PROVIDER)
}