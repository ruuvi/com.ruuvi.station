package com.ruuvi.station.feature

import android.content.Intent
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ruuvi.station.R
import com.ruuvi.station.bluetooth.BluetoothInteractor
import com.ruuvi.station.util.DeviceIdentifier
import com.ruuvi.station.app.preferences.Preferences
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance


class StartupActivity : AppCompatActivity(), KodeinAware {
    override val kodein by closestKodein()
    val bluetoothInteractor: BluetoothInteractor by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startup)
        DeviceIdentifier.id(applicationContext)
        val prefs = Preferences(this)

        if (bluetoothInteractor.canScan())
            bluetoothInteractor.startForegroundScanning()

        if (prefs.isFirstStart) {
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }
        else if (prefs.dashboardEnabled) {
            val intent = Intent(applicationContext, DashboardActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(applicationContext, TagDetailsActivity::class.java)
            intent.putExtra(TagDetailsActivity.FROM_WELCOME,
                    getIntent().getBooleanExtra(TagDetailsActivity.FROM_WELCOME, false)
            )
            startActivity(intent)
        }

        val app = this
        class StartScannerTask:
            AsyncTask<Void, Void, String>() {
            override fun doInBackground(vararg voids: Void): String {
                //MainActivity.setBackgroundScanning(app)
                return "Ok"
            }
            override fun onPostExecute(result: String) {
            }
        }
        StartScannerTask().execute()
    }
}
