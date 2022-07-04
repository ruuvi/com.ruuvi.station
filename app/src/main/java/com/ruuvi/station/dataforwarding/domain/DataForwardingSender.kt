package com.ruuvi.station.dataforwarding.domain

import android.content.Context
import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.ion.Ion
import com.koushikdutta.ion.Response
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.LogReading
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import timber.log.Timber
import java.lang.Exception

class DataForwardingSender(
    private val context: Context,
    private val preferences: PreferencesRepository,
    private val eventFactory: EventFactory
) {
    private val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ").create()

    init {
        Ion.getDefault(context).configure().gson = gson
    }

    fun sendData(tag: RuuviTagEntity, sensorSettings: SensorSettings) {
        val backendUrl = preferences.getDataForwardingUrl()
        if (backendUrl.isNotEmpty()) {
            Timber.d("sendData for ${tag.id} to $backendUrl")

            val event = eventFactory.createEvent(tag, sensorSettings)
            try {
                Ion.with(context)
                    .load(backendUrl)
                    .setLogging("HTTP_LOGS", Log.DEBUG)
                    .setJsonPojoBody(event)
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
    fun sendGattSyncData(readings: List<LogReading>, sensorSettings: SensorSettings) {
        val forwardDuringSync = preferences.getDataForwardingDuringSyncEnabled()
        val backendUrl = preferences.getDataForwardingUrl()
        if (forwardDuringSync && backendUrl.isNotEmpty() && readings.isNotEmpty()) {
            Timber.d("sendBulkData for ${sensorSettings.id}")
            val events: MutableList<LogReading> = ArrayList()
            var i = 0
            for (reading in readings) {
                events.add(reading)
                i++
                if (i >= 100) {
                    sendBatch(events, sensorSettings)
                    events.clear()
                    i = 0
                }
            }
            if (events.isNotEmpty()) {
                sendBatch(events, sensorSettings)
            }
        }
    }

    private fun sendBatch(tags: List<LogReading>, sensorSettings: SensorSettings) {
        val backendUrl = preferences.getDataForwardingUrl()
        val event = eventFactory.createGattEvents(tags, sensorSettings)
        try {
            Ion.with(context)
                    .load(backendUrl)
                    .setLogging("HTTP_LOGS", Log.DEBUG)
                    .setJsonPojoBody(event)
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

    fun test(url: String, deviceId: String, callback: FutureCallback<Response<JsonObject>>) {
        try {
            val scanEvent = eventFactory.createTestEvent()
            Ion.with(context)
                .load(url)
                .setJsonPojoBody(scanEvent)
                .asJsonObject()
                .withResponse()
                .setCallback(callback)
        } catch (e: Exception) {
            callback.onCompleted(e, null)
        }
    }
}