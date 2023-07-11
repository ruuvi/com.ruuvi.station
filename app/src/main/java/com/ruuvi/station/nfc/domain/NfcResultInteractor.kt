package com.ruuvi.station.nfc.domain

import android.content.Context
import android.nfc.NfcManager
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.database.domain.SensorSettingsRepository

class NfcResultInteractor(
    val context: Context,
    val sensorSettingsRepository: SensorSettingsRepository
) {
    val nfcSupported: Boolean by lazy {
        val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        return@lazy manager.defaultAdapter != null
    }

    fun getNfcScanResponse(info: SensorNfсScanInfo): NfcScanResponse {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(info.mac)
        if (sensorSettings != null) {
            return NfcScanResponse(
                id = info.id,
                name = sensorSettings.displayName,
                sensorId = info.mac,
                firmware = info.sw,
                existingSensor = true
            )
        } else {
            return NfcScanResponse(
                id = info.id,
                name = "Ruuvi ${info.mac.takeLast(5).removeRange(2, 3)}",
                sensorId = info.mac,
                firmware = info.sw,
                existingSensor = false
            )
        }
    }
}

data class NfcScanResponse (
    val id: String,
    val name: String,
    val sensorId: String,
    val firmware: String,
    val existingSensor: Boolean
)