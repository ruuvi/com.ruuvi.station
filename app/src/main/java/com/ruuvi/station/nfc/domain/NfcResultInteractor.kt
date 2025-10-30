package com.ruuvi.station.nfc.domain

import android.content.Context
import android.nfc.NfcManager
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.isAir
import com.ruuvi.station.util.MacAddressUtils

class NfcResultInteractor(
    val context: Context,
    val sensorSettingsRepository: SensorSettingsRepository,
    val tagRepository: TagRepository
) {
    val nfcSupported: Boolean by lazy {
        val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
        return@lazy manager.defaultAdapter != null
    }

    fun getNfcScanResponse(info: SensorNfсScanInfo): NfcScanResponse {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(info.mac)
        val canBeAdded = sensorSettings == null
        if (sensorSettings != null) {
            return NfcScanResponse(
                id = info.id,
                name = sensorSettings.displayName,
                sensorId = info.mac,
                firmware = info.sw,
                existingSensor = true,
                canBeAdded = canBeAdded
            )
        } else {
            val tag = tagRepository.getTagById(info.mac)
            return NfcScanResponse(
                id = info.id,
                name = MacAddressUtils.getDefaultName(info.mac, tag?.isAir()),
                sensorId = info.mac,
                firmware = info.sw,
                existingSensor = false,
                canBeAdded = canBeAdded
            )
        }
    }
}

data class NfcScanResponse (
    val id: String,
    val name: String,
    val sensorId: String,
    val firmware: String,
    val existingSensor: Boolean,
    val canBeAdded: Boolean
)