package com.ruuvi.station.util.ui

import android.app.Application
import com.ruuvi.station.about.ui.AboutActivityViewModel
import com.ruuvi.station.addtag.ui.AddTagActivityViewModel
import com.ruuvi.station.app.preferences.PreferencesRepository
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.bindProvider

/** Isolates activity UI tests from production startup, databases, Bluetooth and Firebase. */
class UiTestApplication : Application(), DIAware {
    lateinit var aboutViewModel: AboutActivityViewModel
    lateinit var addTagViewModel: AddTagActivityViewModel
    lateinit var preferences: PreferencesRepository

    override fun onCreate() {
        super.onCreate()
        com.raizlabs.android.dbflow.config.FlowManager.init(this)
    }

    override val di by DI.lazy {
        bindProvider { aboutViewModel }
        bindProvider { addTagViewModel }
        bindProvider { preferences }
    }
}
