package com.ruuvi.station.bluetooth.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class LocationInteractor (private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var lastLocation: Location? = null
    private val timer = Timer("locationUpdateTimer", true)

    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun startLocationUpdate(period: Long) {
        ioScope.launch {
            timer.scheduleAtFixedRate(0, period) {
                updateLocation()
            }
        }
    }

    private fun updateLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                Timber.d("location updated $location")
                lastLocation = location
            }
        }
    }
}