package com.ruuvi.station.network.ui.claim

import com.ruuvi.station.network.ui.claim.ForceClaimNfc.Step
import org.junit.Assert.assertEquals
import org.junit.Test

class ForceClaimNfcTest {
    @Test
    fun `phone without NFC never starts the wait`() {
        assertEquals(Step.NFC_UNAVAILABLE, ForceClaimNfc.nextStep(adapterAvailable = false, adapterEnabled = false))
        assertEquals(Step.NFC_UNAVAILABLE, ForceClaimNfc.nextStep(adapterAvailable = false, adapterEnabled = true))
    }

    @Test
    fun `NFC turned off opens settings instead of waiting`() {
        assertEquals(Step.ENABLE_NFC, ForceClaimNfc.nextStep(adapterAvailable = true, adapterEnabled = false))
    }

    @Test
    fun `NFC turned on starts waiting for the sensor`() {
        assertEquals(Step.START_WAIT, ForceClaimNfc.nextStep(adapterAvailable = true, adapterEnabled = true))
    }
}
