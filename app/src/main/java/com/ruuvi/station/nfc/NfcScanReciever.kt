package com.ruuvi.station.nfc

import android.content.Intent
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import com.ruuvi.gateway.tester.nfc.model.SensorNfсScanInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import kotlin.experimental.and

object NfcScanReciever {
    private val MIME_TEXT_PLAIN = "text/plain"

    private val _nfcSensorScanned = MutableSharedFlow<SensorNfсScanInfo?>()
    val nfcSensorScanned: SharedFlow<SensorNfсScanInfo?> = _nfcSensorScanned

    fun nfcScanned(intent: Intent) {
        if (intent.type == MIME_TEXT_PLAIN) {
            val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)

            tag?.let {
                val sensorInfo = parseIt(tag)
                sensorInfo?.let {
                    Timber.d("NFC scan result: $sensorInfo")
                    CoroutineScope(Dispatchers.Main).launch {
                        _nfcSensorScanned.emit(sensorInfo)
                    }
                }
            }
        }
    }

    private fun parseIt(tag: Tag): SensorNfсScanInfo? {
        try {
            // NDEF is not supported by this Tag if it's null
            val ndef = Ndef.get(tag) ?: return null

            var id: String? = null
            var mac: String? = null
            var sw: String? = null
            for (ndefRecord in ndef.cachedNdefMessage.records) {
                val decode = parseNdefRecord(ndefRecord)
                when (decode?.first) {
                    "id" -> id = decode.second
                    "ad" -> mac = decode.second
                    "sw" -> sw = decode.second
                }
            }
            if (id != null && mac != null && sw != null) {
                return SensorNfсScanInfo(
                    id.replace("ID: ", ""),
                    mac.replace("MAC: ", ""),
                    sw.replace("SW: ", "")
                )
            }
        } catch (e: UnsupportedEncodingException) {
            Timber.e(e)
        }
        return null
    }

    private fun parseNdefRecord(ndefRecord: NdefRecord): Pair<String,String>? {
        var result: Pair<String,String>? = null
        try {
            val payload = ndefRecord.payload
            val textEncoding = if (payload[0] and 128.toByte() == 0.toByte()) "UTF-8" else "UTF-16"
            val languageSize = payload[0] and 51
            var end = payload.size
            for (i in 0.until(payload.size - 1)) {
                val b = payload[i].toInt()
                if (b == 0) {
                    end = i
                    break
                }
            }
            end = end - languageSize - 1
            val language = String(
                payload,
                1,
                languageSize.toInt(),
                Charset.forName(textEncoding)
            )
            val value = String(
                payload,
                languageSize + 1,
                end,
                Charset.forName(textEncoding)
            )
            result = language to value
        } catch (e: Exception) {
            Timber.e(e)
        }
        return result
    }
}