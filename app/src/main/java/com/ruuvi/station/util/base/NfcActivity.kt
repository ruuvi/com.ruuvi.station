package com.ruuvi.station.util.base

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.tech.NfcA
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.ruuvi.station.nfc.NfcScanReciever

open class NfcActivity(): AppCompatActivity() {
    constructor (contentLayoutId: Int): this() {
        AppCompatActivity(contentLayoutId)
    }

    private var nfcAdapter: NfcAdapter? = null

    private val intentFiltersArray = arrayOf(
        IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("text/plain")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("fail", e)
            }}
    )

    private val techListsArray = arrayOf(arrayOf<String>(NfcA::class.java.name))

    private var nfcIntent : Intent? = null

    private var pendingIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        nfcIntent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        pendingIntent = PendingIntent.getActivity(
            this, 0, nfcIntent,
            PendingIntent.FLAG_MUTABLE
        )

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.type == "text/plain" && intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            NfcScanReciever.nfcScanned(intent)
        }
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, techListsArray)

    }
}