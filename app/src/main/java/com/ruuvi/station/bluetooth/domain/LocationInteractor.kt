package com.ruuvi.station.bluetooth.domain

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import com.ruuvi.station.dataforwarding.data.ScanLocation
import timber.log.Timber
import java.util.*

class LocationInteractor (private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationUpdateDate: Long = Long.MIN_VALUE
    private var lastLocation: Location? = null

    fun getLocation(): ScanLocation? {
        Timber.d("getLocation")
        updateLocation()
        val location = lastLocation
        return if (location == null) {
            null
        } else {
            ScanLocation(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = location.altitude,
                accuracy = location.accuracy
            )
        }
    }

    private fun updateLocation() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, -1)
        val updateThreshold = calendar.time.time
        if (lastLocation == null || locationUpdateDate < updateThreshold) {
            locationUpdateDate = Date().time
            val permissionGranted = ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            Timber.d("updateLocation permissionGranted = $permissionGranted")
            if (permissionGranted) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    Timber.d("location updated $location")
                    lastLocation = location
                    if (location == null) {
                        requestLocation()
                    }
                }
            }
        }
    }

    @RequiresPermission(ACCESS_COARSE_LOCATION)
    private fun requestLocation() {
        fusedLocationClient.getCurrentLocation(com.google.android.gms.location.LocationRequest.PRIORITY_LOW_POWER, object: CancellationToken() {
            override fun isCancellationRequested(): Boolean {
                return false
            }

            override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                return this
            }
        })
    }
}