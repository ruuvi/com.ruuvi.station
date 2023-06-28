package com.ruuvi.station.nfc.domain

import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.database.domain.SensorSettingsRepository

class NfcResultInteractor(
    val sensorSettingsRepository: SensorSettingsRepository
) {

    fun getNfcScanResponse(info: SensorNfсScanInfo): NfcScanResponse {
        val sensorSettings = sensorSettingsRepository.getSensorSettings(info.mac)
        if (sensorSettings != null) {
            return NfcScanResponse.ExistingSensor(info.mac)
        } else {
            return NfcScanResponse.NewSensor(
                id = info.id,
                name =  "Ruuvi ${info.mac.takeLast(5).removeRange(2,3)}",
                sensorId = info.mac,
                firmware = info.sw
            )
        }
    }
}

sealed class NfcScanResponse {

    data class ExistingSensor(
        val sensorId: String
    ) : NfcScanResponse()

    data class NewSensor(
        val id: String,
        val name: String,
        val sensorId: String,
        val firmware: String
    ) : NfcScanResponse()
}