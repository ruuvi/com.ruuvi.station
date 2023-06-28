package com.ruuvi.station.addtag.ui

import androidx.lifecycle.ViewModel
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.nfc.domain.NfcResultInteractor
import com.ruuvi.station.tag.domain.TagInteractor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar

class AddTagActivityViewModel(
    private val tagInteractor: TagInteractor,
    private val nfcResultInteractor: NfcResultInteractor
) : ViewModel() {

    val sensorFlow = flow{
        while (true) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, -60)

            val tagsDb = tagInteractor.getTagEntities(false)
                .mapNotNull { tag ->
                    if (tag.updateAt?.time?.compareTo(calendar.time.time) == -1) {
                        null
                    } else {
                        tag
                    }
                }
                .sortedByDescending { tag -> tag.rssi }
            emit(tagsDb)
            delay(3000)
        }
    }
        .flowOn(Dispatchers.IO)


    fun getTagById(tagId: String): RuuviTagEntity? =
        tagInteractor.getTagEntityById(tagId)

    fun makeSensorFavorite(sensor: RuuviTagEntity) {
        tagInteractor.makeSensorFavorite(sensor)
    }

    fun getNfcScanResponse(scanInfo: SensorNfсScanInfo) = nfcResultInteractor.getNfcScanResponse(scanInfo)

    fun addSensor(sensorId: String) {
        tagInteractor.getTagEntityById(sensorId)?.let {
            tagInteractor.makeSensorFavorite(it)
        }
    }
}