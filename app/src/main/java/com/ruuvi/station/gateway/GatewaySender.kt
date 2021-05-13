package com.ruuvi.station.gateway

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.Response
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.gateway.data.ScanEvent
import com.ruuvi.station.gateway.data.ScanLocation
import timber.log.Timber
import java.lang.Exception

class GatewaySender(
    private val context: Context,
    private val preferences: PreferencesRepository
) {
    private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ").create()

    init {
        Ion.getDefault(context).configure().gson = gson
    }

    fun sendData(tag: RuuviTagEntity, location: Location?) {
        val backendUrl = preferences.getGatewayUrl()
        if (backendUrl.isNotEmpty()) {
            Timber.d("sendData for ${tag.id} to $backendUrl")

            var scanLocation: ScanLocation? = null
            location?.let {
                scanLocation = ScanLocation(
                    it.latitude,
                    it.longitude,
                    it.accuracy
                )
            }

            val deviceId = preferences.getDeviceId()
            val eventBatch = ScanEvent(context, deviceId)
            eventBatch.location = scanLocation
            eventBatch.tags.add(tag)
            try {
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
            } catch (e: Exception) {
                Timber.e(e, "Batch sending failed to $backendUrl")
            }
        }
    }

    fun test(gatewayUrl: String, deviceId: String, callback: FutureCallback<Response<JsonObject>>) {
        try {
            val scanEvent = ScanEvent(context, deviceId)
            Ion.with(context)
                .load(gatewayUrl)
                .setJsonPojoBody(scanEvent)
                .asJsonObject()
                .withResponse()
                .setCallback(callback)
        } catch (e: Exception) {
            callback.onCompleted(e, null)
        }
    }
}