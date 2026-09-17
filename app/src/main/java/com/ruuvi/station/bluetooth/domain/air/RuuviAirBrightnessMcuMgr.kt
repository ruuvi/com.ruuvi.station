package com.ruuvi.station.bluetooth.domain.air

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.ruuvi.station.tagsettings.ui.led_control.LedBrightnessLevel
import no.nordicsemi.android.mcumgr.McuMgrCallback
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.ble.McuMgrBleTransport
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.mcumgr.managers.ShellManager
import no.nordicsemi.android.mcumgr.response.shell.McuMgrExecResponse
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class RuuviAirBrightnessMcuMgr(
    context: Context,
    mac: String,
    bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
) : AutoCloseable {

    private val device: BluetoothDevice = bluetoothAdapter.getRemoteDevice(mac)
    private val transport: McuMgrTransport = McuMgrBleTransport(context.applicationContext, device)
    private val shell = ShellManager(transport)

    suspend fun setBrightness(level: LedBrightnessLevel): McuMgrExecResponse {
        val cmd = "ruuvi led_brightness ${level.commandLevel}"
        Timber.d("setBrightness cmd=$cmd")
        val resp = exec(cmd)
        Timber.d("setBrightness resp rc=${resp.rc} o=${resp.o} ")
        return resp
    }

    suspend fun exec(command: String): McuMgrExecResponse =
        suspendCancellableCoroutine { cont ->
            shell.exec(command, arrayOf(), object : McuMgrCallback<McuMgrExecResponse> {
                override fun onResponse(response: McuMgrExecResponse) {
                    cont.resume(response)
                }

                override fun onError(error: McuMgrException) {
                    cont.resumeWithException(error)
                }

            })

            cont.invokeOnCancellation { }
        }

    override fun close() {
        try {
            transport.release()
        } catch (t: Throwable) {
            Timber.e(t)
        }
    }
}