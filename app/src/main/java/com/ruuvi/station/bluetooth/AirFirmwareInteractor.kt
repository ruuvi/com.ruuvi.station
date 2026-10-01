package com.ruuvi.station.bluetooth

import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import no.nordicsemi.android.mcumgr.McuMgrCallback
import no.nordicsemi.android.mcumgr.ble.McuMgrBleTransport
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.mcumgr.managers.DefaultManager
import no.nordicsemi.android.mcumgr.managers.FsManager
import no.nordicsemi.android.mcumgr.response.dflt.McuMgrEchoResponse
import no.nordicsemi.android.mcumgr.response.dflt.McuMgrOsResponse
import no.nordicsemi.android.mcumgr.transfer.FileUploader
import no.nordicsemi.android.mcumgr.transfer.UploadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import no.nordicsemi.android.ble.ConnectionPriorityRequest
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume

class AirFirmwareInteractor (
    val context: Context,
){
    private var defaultManager: DefaultManager? = null

    suspend fun connect(address: String): Boolean {
        Timber.d("Creating device for $address")
        val device = context.getSystemService(BluetoothManager::class.java).adapter.getRemoteDevice(address)
        Timber.d("Creating transport for $device")
        val transport = McuMgrBleTransport(context,  device)
        Timber.d("Creating DefaultManager for $transport")
        defaultManager = DefaultManager(transport)
        Timber.d("Sending echo with $defaultManager")
        return suspendCancellableCoroutine<Boolean> { cont ->
            defaultManager?.echo("Hello!", object : McuMgrCallback<McuMgrEchoResponse> {
                override fun onResponse(p0: McuMgrEchoResponse) {
                    Timber.d("AirFirmwareInteractor onResponse $p0")
                    cont.resume(true)
                }

                override fun onError(p0: McuMgrException) {
                    Timber.d("AirFirmwareInteractor onError $p0")
                    cont.resume(false)
                }
            }) ?: cont.resume(false)
        }
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
        resetOnDone: Boolean,
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
                        if (resetOnDone) reset()
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