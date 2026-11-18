package com.ruuvi.station.network.ui.claim

internal object ForceClaimNfc {

    enum class Step { NFC_UNAVAILABLE, ENABLE_NFC, START_WAIT }

    fun nextStep(adapterAvailable: Boolean, adapterEnabled: Boolean): Step =
        when {
            !adapterAvailable -> Step.NFC_UNAVAILABLE
            !adapterEnabled -> Step.ENABLE_NFC
            else -> Step.START_WAIT
        }
}
