package com.ruuvi.station.gateway

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.GsonBuilder
import com.koushikdutta.ion.Ion
import com.ruuvi.station.app.preferences.Preferences
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.gateway.data.ScanEvent
import com.ruuvi.station.gateway.data.ScanEventSingle
import com.ruuvi.station.gateway.data.ScanLocation
import timber.log.Timber

class GatewaySender (private val context: Context, private val preferences: Preferences) {
    private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").create()

    fun sendData(tag: RuuviTagEntity, location: Location?){
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

        if (!tag.gatewayUrl.isNullOrEmpty()) {
            val single = ScanEventSingle(context)
            single.location = scanLocation
            single.tag = tag
            Ion.with(context)
                    .load(tag.gatewayUrl)
                    .setJsonPojoBody(single)
                    .asJsonObject()
                    .setCallback { e, _ ->
                        if (e != null) {
                            Timber.e(e, "Sending failed [${tag.name}] (${tag.id}) to ${tag.gatewayUrl}")
                        }
                    }
        }

        val backendUrl = preferences.gatewayUrl
        if (backendUrl.isNotEmpty()) {
            val eventBatch = ScanEvent(context)
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