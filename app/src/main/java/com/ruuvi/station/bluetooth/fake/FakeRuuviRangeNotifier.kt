package com.ruuvi.station.bluetooth.fake

import android.os.Handler
import com.ruuvi.station.bluetooth.FoundRuuviTag
import com.ruuvi.station.bluetooth.IRuuviRangeNotifier

class FakeRuuviRangeNotifier : IRuuviRangeNotifier {

    private var isScanning: Boolean = false

    private var scanInterval: Long? = null
    private var isBackgroundModeEnabled = false

    override fun enableBackgroundMode(isBackgroundModeEnabled: Boolean) {
        this.isBackgroundModeEnabled = isBackgroundModeEnabled
    }

    override fun enableScheduledScans(areScheduledScansEnabled: Boolean) {
    }

    override fun getBackgroundScanInterval(): Long? = scanInterval

    override fun setBackgroundScheduledScanInterval(scanInterval: Long) {
        this.scanInterval = scanInterval
    }

    override fun startScanning(tagsFoundListener: IRuuviRangeNotifier.OnTagsFoundListener, shouldLaunchInBackground: Boolean) {
        this.isBackgroundModeEnabled = shouldLaunchInBackground
        this.isScanning = true

        findRuuviTags(tagsFoundListener)
    }

    private fun findRuuviTags(tagsFoundListener: IRuuviRangeNotifier.OnTagsFoundListener) {
        Handler().postDelayed(
            {
                tagsFoundListener.onTagsFound(
                    getRuuviTags()
                )

                if (isScanning) {
                    findRuuviTags(tagsFoundListener)
                }
            },
            1000
        )
    }

    private fun getRuuviTags(): List<FoundRuuviTag> {

        val tag1 = FoundRuuviTag()
        tag1.id = "1"
        tag1.accelX = 1.0
        tag1.accelY = 1.0
        tag1.accelZ = 1.0
        tag1.dataFormat = 1
        tag1.humidity = 10.0
        tag1.pressure = 10.0
        tag1.rssi = 1
        tag1.temperature = 36.6
        tag1.txPower = 10.0
        tag1.url = "1"
        tag1.voltage = 10.0

        val tag2 = FoundRuuviTag()
        tag2.id = "2"
        tag2.accelX = 2.0
        tag2.accelY = 2.0
        tag2.accelZ = 2.0
        tag2.dataFormat = 2
        tag2.humidity = 20.0
        tag2.pressure = 20.0
        tag2.rssi = 2
        tag2.temperature = 36.6
        tag2.txPower = 20.0
        tag2.url = "2"
        tag2.voltage = 20.0

        val tag3 = FoundRuuviTag()
        tag3.id = "3"
        tag3.accelX = 3.0
        tag3.accelY = 3.0
        tag3.accelZ = 3.0
        tag3.dataFormat = 3
        tag3.humidity = 30.0
        tag3.pressure = 30.0
        tag3.rssi = 3
        tag3.temperature = 36.6
        tag3.txPower = 30.0
        tag3.url = "3"
        tag3.voltage = 30.0

        return listOf(
            tag1,
            tag2,
            tag3
        )
    }

    override fun stopScanning() {
        isScanning = false
    }
}