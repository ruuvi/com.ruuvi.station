package com.ruuvi.station.util.test

import android.os.Handler
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.FoundRuuviTag
import timber.log.Timber

class FakeScanResultsSender(private val defaultOnTagFoundListener: DefaultOnTagFoundListener) {
    private val handler = Handler()

    var sender = object:Runnable {
        override fun run() {
            Timber.d("Sending fake tag data with interval = $interval")
            defaultOnTagFoundListener.onTagFound(getTagInfo())
            handler.postDelayed(this, interval)
        }
    }

    init {
        Timber.d("FakeScanResultsSender initialized")
    }

    fun startSendFakes() {
        handler.postDelayed(sender, interval)
    }

    fun stopSendFakes() {
        handler.removeCallbacks(sender)
    }

    private fun getTagInfo(): FoundRuuviTag {
        return FoundRuuviTag().apply {
            id = "FAKE:FAKE:FAKE"
            accelX = -0.013
            accelY = 0.013
            accelZ = 1.046
            dataFormat = 3
            humidity = 43.0
            pressure = 101371.0
            rssi = -73
            temperature = 25.71
            voltage = 2.995
        }
    }

    companion object {
        const val interval = 10000L
    }
}