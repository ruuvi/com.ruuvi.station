package com.ruuvi.station.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import io.runtime.mcumgr.McuMgrCallback
import io.runtime.mcumgr.ble.McuMgrBleTransport
import io.runtime.mcumgr.exception.McuMgrException
import io.runtime.mcumgr.managers.DefaultManager
import io.runtime.mcumgr.managers.FsManager
import io.runtime.mcumgr.response.dflt.McuMgrEchoResponse
import io.runtime.mcumgr.response.dflt.McuMgrOsResponse
import io.runtime.mcumgr.transfer.FileUploader
import io.runtime.mcumgr.transfer.UploadCallback
import no.nordicsemi.android.ble.ConnectionPriorityRequest
import timber.log.Timber
import java.io.File

class AirFirmwareInteractor (
    val context: Context,
){
    private var defaultManager: DefaultManager? = null

    fun connect(address: String) {
        Timber.d("Creating device for $address")
        val device = context.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice(address)
        Timber.d("Creating transport for $device")
        val transport = McuMgrBleTransport(context,  device)
        Timber.d("Creating DefaultManager for $transport")
        defaultManager = DefaultManager(transport)
        Timber.d("Sending echo with $defaultManager")
        defaultManager?.echo("Hello!", object : McuMgrCallback<McuMgrEchoResponse> {
            override fun onResponse(p0: McuMgrEchoResponse) {
                Timber.d("AirFirmwareInteractor onResponse $p0")
            }

            override fun onError(p0: McuMgrException) {
                Timber.d("AirFirmwareInteractor onError $p0")
            }
        })
    }

    private fun requestHighConnectionPriority() {
        Timber.d("requestHighConnectionPriority")

        val transporter = defaultManager?.transporter
        if (transporter is McuMgrBleTransport) {
            transporter.requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH)
        }
    }

    private fun setLoggingEnabled(enabled: Boolean) {
        Timber.d("setLoggingEnabled $enabled")

        val transporter = defaultManager?.transporter
        if (transporter is McuMgrBleTransport) {
            transporter.setLoggingEnabled(enabled)
        }
    }

    fun upload(
        file: File,
        progress: (Int, Int) -> Unit,
        done: () -> Unit,
        fail: (String) -> Unit
    ) {
        val path = "/lfs1/${file.name}"

        val data = readBytesFromUri(context, file.toUri())

        Timber.d("upload dest $path defaultManager $defaultManager size ${data?.size}")

        data?.let {
            requestHighConnectionPriority()
            setLoggingEnabled(false)

            val controller = FileUploader(FsManager(defaultManager!!.transporter), path, data, 3, 4)
                .uploadAsync(object : UploadCallback {
                    override fun onUploadProgressChanged(p0: Int, p1: Int, p2: Long) {
                        Timber.d("AirFirmwareInteractor onUploadProgressChanged $p0, $p1, $p2")
                        progress(p0, p1)
                    }

                    override fun onUploadFailed(p0: McuMgrException) {
                        Timber.d("AirFirmwareInteractor onUploadFailed $p0")
                        fail(p0.message ?: "")
                    }

                    override fun onUploadCanceled() {
                        Timber.d("AirFirmwareInteractor onUploadCanceled")
                    }

                    override fun onUploadCompleted() {
                        Timber.d("AirFirmwareInteractor onUploadCompleted")
                        reset()
                        done()
                    }
                })
        }
    }

    fun reset() {
        defaultManager?.reset(object: McuMgrCallback<McuMgrOsResponse> {
            override fun onResponse(p0: McuMgrOsResponse) {
                Timber.d("AirFirmwareInteractor reset onResponse $p0")
            }

            override fun onError(p0: McuMgrException) {
                Timber.d("AirFirmwareInteractor reset onError $p0")
            }

        })
    }

    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}