package com.ruuvi.station.bluetooth.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.dfu.domain.DfuService
import no.nordicsemi.android.dfu.DfuProgressListener
import no.nordicsemi.android.dfu.DfuServiceInitiator
import no.nordicsemi.android.dfu.DfuServiceListenerHelper
import timber.log.Timber
import java.io.File

class DfuInteractor(val context: Context) {
    var statusCallback: ((DfuUpdateStatus)->Unit)? = null

    fun startDfuUpdate(sensorId: String, fwFile: File, status: (DfuUpdateStatus)->Unit) {
        statusCallback = status

        val initiator = DfuServiceInitiator(sensorId)
            .setKeepBond(true)

        initiator.setZip(fwFile.absolutePath)
        initiator.setDisableNotification(true)
        initiator.setForeground(false)

        val controller = initiator.start(context, DfuService::class.java)
    }

    val dfuProgressListener = object : DfuProgressListener {
        override fun onDeviceConnecting(p0: String) {
            Timber.d("onDeviceConnecting $p0")
        }

        override fun onDeviceConnected(p0: String) {
            Timber.d("onDeviceConnected $p0")
        }

        override fun onDfuProcessStarting(p0: String) {
            Timber.d("onDfuProcessStarting $p0")
        }

        override fun onDfuProcessStarted(p0: String) {
            Timber.d("onDfuProcessStarted $p0")
        }

        override fun onEnablingDfuMode(p0: String) {
            Timber.d("onEnablingDfuMode $p0")
        }

        override fun onProgressChanged(
            p0: String,
            percent: Int,
            p2: Float,
            p3: Float,
            currentPart: Int,
            partsTotal: Int
        ) {
            Timber.d("onProgressChanged $p0, $percent, $p2, $p3, $currentPart, $partsTotal")
            val progress = (percent + (100 * currentPart - 100)).toDouble()/(100 * partsTotal) * 100
            statusCallback?.invoke(DfuUpdateStatus.Progress(progress.toInt()))
        }

        override fun onFirmwareValidating(p0: String) {
            Timber.d("onFirmwareValidating $p0")
        }

        override fun onDeviceDisconnecting(p0: String?) {
            Timber.d("onDeviceDisconnecting $p0")
        }

        override fun onDeviceDisconnected(p0: String) {
            Timber.d("onDeviceDisconnected $p0")
        }

        override fun onDfuCompleted(p0: String) {
            Timber.d("onDfuCompleted $p0")
            statusCallback?.invoke(DfuUpdateStatus.Finished(true))
        }

        override fun onDfuAborted(p0: String) {
            Timber.d("onDfuAborted $p0")
        }

        override fun onError(p0: String, p1: Int, p2: Int, p3: String?) {
            Timber.d("onError $p0 $p3")
            statusCallback?.invoke(DfuUpdateStatus.Error(tryToLocalizeMessage(p3)))
        }
    }

    private fun tryToLocalizeMessage(message: String?): String? =
        when (message) {
            DFU_DEVICE_DISCONNECTED -> context.getString(R.string.error_dfu_disconnected)
            GATT_ERROR -> context.getString(R.string.error_dfu_update)
            DFU_FILE_NOT_FOUND -> context.getString(R.string.error_dfu_firmware_not_valid)
            OPERATION_FAILED -> context.getString(R.string.error_dfu_firmware_not_valid)
            else -> message
        }

    init {
        DfuServiceListenerHelper.registerProgressListener(context, dfuProgressListener)
    }

    companion object {
        const val DFU_DEVICE_DISCONNECTED = "DFU DEVICE DISCONNECTED"
        const val GATT_ERROR = "GATT ERROR"
        const val DFU_FILE_NOT_FOUND = "DFU FILE NOT FOUND"
        const val OPERATION_FAILED =  "OPERATION FAILED"
    }
}

sealed class DfuUpdateStatus{
    data class Progress(val percent: Int): DfuUpdateStatus()
    data class Finished(val success: Boolean): DfuUpdateStatus()
    data class Error(val message: String?): DfuUpdateStatus()
}