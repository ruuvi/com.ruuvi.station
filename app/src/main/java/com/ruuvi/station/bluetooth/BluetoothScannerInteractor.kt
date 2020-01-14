package com.ruuvi.station.bluetooth

import android.app.Application
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.ruuvi.station.gateway.Http
import com.ruuvi.station.model.RuuviTag
import com.ruuvi.station.model.TagSensorReading
import com.ruuvi.station.util.AlarmChecker
import com.ruuvi.station.util.Foreground
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap

class BluetoothScannerInteractor(private val application: Application) {

    private val TAG: String = BluetoothScannerInteractor::class.java.simpleName

    private val backgroundTags = ArrayList<RuuviTag>()

    private val lastLogged: MutableMap<String, Long> = HashMap()
    private val LOG_INTERVAL = 5 // seconds

    private var scanning = false

    private var foreground: Boolean = true.also {
        val listener: Foreground.Listener = object : Foreground.Listener {
            override fun onBecameForeground() {
                foreground = true
            }

            override fun onBecameBackground() {
                foreground = false
            }
        }

        Foreground.init(application)
        Foreground.get().addListener(listener)
    }

    private val ruuviTagScanner by lazy {
        RuuviTagScanner(
            RuuviTagListener { logTag(it, application, foreground) },
            application
        )
    }

    fun logTag(ruuviTag: RuuviTag, context: Context?, foreground: Boolean) {
        var ruuviTag = ruuviTag
        val dbTag = RuuviTag.get(ruuviTag.id)
        if (dbTag != null) {
            ruuviTag = dbTag.preserveData(ruuviTag)
            ruuviTag.update()
            if (!dbTag.favorite) return
        } else {
            ruuviTag.updateAt = Date()
            ruuviTag.save()
            return
        }
        if (!foreground) {
            if (ruuviTag.favorite && checkForSameTag(backgroundTags, ruuviTag) == -1) {
                backgroundTags.add(ruuviTag)
            }
            return
        }
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.SECOND, -LOG_INTERVAL)
        val loggingThreshold = calendar.time.time
        for ((key, value) in lastLogged) {
            if (key == ruuviTag.id && value > loggingThreshold) {
                return
            }
        }
        val tags: MutableList<RuuviTag> = ArrayList()
        tags.add(ruuviTag)
        Http.post(tags, null, context)
        lastLogged[ruuviTag.id] = Date().time
        val reading = TagSensorReading(ruuviTag)
        reading.save()
        AlarmChecker.check(ruuviTag, context)
    }

    fun getBackgroundTags(): List<RuuviTag> = backgroundTags

    fun clearBackgroundTags() {
        backgroundTags.clear()
    }

    fun startScan() {
        if (scanning || !ruuviTagScanner.canScan()) return
        scanning = true
        try {
            ruuviTagScanner.start()
        } catch (e: Exception) {
            Log.e(TAG, e.message)
            scanning = false
            Toast.makeText(application, "Couldn't start scanning, is bluetooth disabled?", Toast.LENGTH_LONG).show()
        }
    }

    fun stopScan() {
        if (!ruuviTagScanner.canScan()) return
        scanning = false
        ruuviTagScanner.stop()
    }

    private fun checkForSameTag(arr: List<RuuviTag>, ruuvi: RuuviTag): Int {
        for (i in arr.indices) {
            if (ruuvi.id == arr[i].id) {
                return i
            }
        }
        return -1
    }
}