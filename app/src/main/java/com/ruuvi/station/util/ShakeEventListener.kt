package com.ruuvi.station.util

import android.hardware.*
import com.ruuvi.station.util.extensions.diffGreaterThan
import timber.log.Timber
import java.util.*

class ShakeEventListener(
    var shakeCallback: (Int) -> Unit
): SensorEventListener {
    private var acceleration = 10f
    private var currentAcceleration = SensorManager.GRAVITY_EARTH
    private var lastAcceleration = SensorManager.GRAVITY_EARTH
    private var lastShake: Date? = null
    private var shakeCount: Int = 0

    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        lastAcceleration = currentAcceleration
        currentAcceleration = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val delta: Float = currentAcceleration - lastAcceleration
        acceleration = acceleration * 0.9f + delta
        Timber.d("onSensorChanged $acceleration")

        if (acceleration > 12 && lastShake?.diffGreaterThan(2500) ?: true)  {
            shakeCount++
            lastShake = Date()
            Timber.d("Shake event detected $shakeCount")
            shakeCallback.invoke(shakeCount)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}