package com.ruuvi.station.util.test

import android.os.Handler
import com.ruuvi.station.bluetooth.DefaultOnTagFoundListener
import com.ruuvi.station.bluetooth.FoundRuuviTag
import timber.log.Timber
import java.util.Locale
import kotlin.random.Random

class FakeScanResultsSender(private val defaultOnTagFoundListener: DefaultOnTagFoundListener) {
    private val handler = Handler()

    var sender = object : Runnable {
        override fun run() {
            Timber.d("Sending fake tag data with interval = $INTERVAL")
            defaultOnTagFoundListener.onTagFound(getTagInfo())
            handler.postDelayed(this, INTERVAL)
        }
    }

    init {
        Timber.d("FakeScanResultsSender initialized")
    }

    fun startSendFakes() {
        handler.postDelayed(sender, INTERVAL)
    }

    fun stopSendFakes() {
        handler.removeCallbacks(sender)
    }

    private fun getTagInfo(): FoundRuuviTag {
        return FoundRuuviTag().apply {
            id = getRandomMacAddress()
            accelX = -0.013
            accelY = 0.013
            accelZ = 1.046
            dataFormat = 3
            humidity = 43.0
            pressure = 101371.0
            rssi = Random.nextInt(-100, 0)
            temperature = 25.71
            voltage = 2.995
        }
    }

    private fun getRandomMacAddress(): String? {
        var mac = FAKE_PREFIX
        for (i in 0..3) {
            val n = Random.nextInt(255)
            mac += String.format(MAC_ADDRESS_FORMAT, n)
        }
        return mac.uppercase(Locale.getDefault())
    }

    companion object {
        private const val FAKE_PREFIX = "FA:KE"
        private const val MAC_ADDRESS_FORMAT = ":%02x"
        private const val INTERVAL = 10 * 1000L // 10 seconds
    }
}