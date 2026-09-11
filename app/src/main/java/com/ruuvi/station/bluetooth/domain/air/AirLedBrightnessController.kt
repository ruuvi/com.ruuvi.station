package com.ruuvi.station.bluetooth.domain.air

import android.bluetooth.BluetoothAdapter
import android.content.Context
import com.ruuvi.station.tagsettings.ui.led_control.LedBrightnessLevel
import no.nordicsemi.android.mcumgr.response.shell.McuMgrExecResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AirLedBrightnessController(
    private val appContext: Context,
    private val bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter(),
) {
    private val mutex = Mutex()

    suspend fun setBrightness(mac: String, level: LedBrightnessLevel): McuMgrExecResponse =
        mutex.withLock {
            retry(times = 3, initialDelayMs = 250) {
                RuuviAirBrightnessMcuMgr(appContext, mac, bluetoothAdapter).useAutoCloseable { mgr ->
                    mgr.setBrightness(level)
                }
            }
        }

    private suspend fun <T> retry(
        times: Int,
        initialDelayMs: Long,
        block: suspend () -> T
    ): T {
        var delayMs = initialDelayMs
        var last: Throwable? = null

        repeat(times) { attempt ->
            try {
                return block()
            } catch (t: Throwable) {
                last = t
                if (attempt == times - 1) throw t
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(2000)
            }
        }
        throw last ?: IllegalStateException("Retry failed")
    }

    inline fun <T : AutoCloseable, R> T.useAutoCloseable(block: (T) -> R): R =
        try { block(this) } finally { runCatching { close() } }
}