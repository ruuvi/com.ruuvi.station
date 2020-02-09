package com.ruuvi.station.bluetooth.fake

import android.os.Handler
import com.ruuvi.station.bluetooth.FoundRuuviTag
import com.ruuvi.station.bluetooth.IRuuviTagScanner

class FakeRuuviTagScanner : IRuuviTagScanner {

    private var isScanning = false

    override fun canScan(): Boolean = !isScanning

    override fun startScanning(foundListener: IRuuviTagScanner.OnTagFoundListener) {

        isScanning = true

        val handler = Handler()

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


        handler.postDelayed(
            {
                foundListener.onTagFound(
                    tag1
                )
            },
            1000
        )

        handler.postDelayed(
            {
                foundListener.onTagFound(
                    tag2
                )
            },
            2000
        )

        handler.postDelayed(
            {
                foundListener.onTagFound(
                    tag3
                )
            },
            3000
        )
    }

    override fun stopScanning() {
        isScanning = false
    }
}