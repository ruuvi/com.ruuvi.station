package com.ruuvi.station.history

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/** Synthetic graph tests do not start cloud sync, Bluetooth, or account services. */
class HistoryTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader, className: String, context: Context): Application =
        super.newApplication(cl, Application::class.java.name, context)
}
