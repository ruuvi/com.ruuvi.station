package com.ruuvi.station.dfu.domain

import android.app.Activity
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.dfu.ui.DfuStatusActivity
import no.nordicsemi.android.dfu.DfuBaseService

class DfuService: DfuBaseService() {
    override fun getNotificationTarget(): Class<out Activity> {
        return DfuStatusActivity::class.java
    }

    override fun isDebug(): Boolean {
        return BuildConfig.DEBUG
    }
}