package com.ruuvi.station.bluetooth.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.ruuvi.station.gateway.data.ScanLocation
import timber.log.Timber
import java.util.*

class LocationInteractor (private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationUpdateDate: Long = Long.MIN_VALUE
    private var lastLocation: Location? = null

    fun getLocation(): ScanLocation? {
        updateLocation()
        val location = lastLocation
        return if (location == null) {
            null
        } else {
            ScanLocation(location.latitude, location.longitude, location.accuracy)
        }
    }

    private fun updateLocation() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -1)
        val updateThreshold = calendar.time.time
        if (lastLocation == null || locationUpdateDate < updateThreshold) {
            locationUpdateDate = Date().time
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    Timber.d("location updated $location")
                    lastLocation = location
                }
            }
        }
    }
}