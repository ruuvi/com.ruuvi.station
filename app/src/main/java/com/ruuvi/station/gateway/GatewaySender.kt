package com.ruuvi.station.gateway

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.GsonBuilder
import com.koushikdutta.ion.Ion
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.gateway.data.ScanEvent
import com.ruuvi.station.gateway.data.ScanLocation
import timber.log.Timber

class GatewaySender(
    private val context: Context,
    private val preferences: PreferencesRepository
) {
    private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()

    fun sendData(tag: RuuviTagEntity, location: Location?) {
        val backendUrl = preferences.getGatewayUrl()
        if (backendUrl.isNotEmpty()) {
            Timber.d("sendData for [${tag.name}] (${tag.id}) to ${tag.gatewayUrl}")

            var scanLocation: ScanLocation? = null
            location?.let {
                scanLocation = ScanLocation(
                    it.latitude,
                    it.longitude,
                    it.accuracy
                )
            }

            Ion.getDefault(context).configure().gson = gson
            val deviceId = preferences.getDeviceId()
            val eventBatch = ScanEvent(context, deviceId)
            eventBatch.location = scanLocation
            eventBatch.tags.add(tag)
            Ion.with(context)
                .load(backendUrl)
                .setLogging("HTTP_LOGS", Log.DEBUG)
                .setJsonPojoBody(eventBatch)
                .asJsonObject()
                .setCallback { e, _ ->
                    if (e != null) {
                        Timber.e(e, "Batch sending failed to $backendUrl")
                    }
                }
        }
    }
}