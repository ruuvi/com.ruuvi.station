package com.ruuvi.station.network.ui.claim

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.os.Build
import com.ruuvi.station.network.ui.claim.ForceClaimBluetooth.Step
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ForceClaimBluetoothTest {
    @Test
    fun `missing permissions are requested before anything else`() {
        assertEquals(Step.REQUEST_PERMISSIONS, step(permissionsGranted = false, adapterEnabled = true))
        assertEquals(Step.REQUEST_PERMISSIONS, step(permissionsGranted = false, adapterEnabled = false))
        assertEquals(Step.REQUEST_PERMISSIONS, step(permissionsGranted = false, adapterAvailable = false))
    }

    @Test
    fun `disabled adapter is enabled before the read starts`() {
        assertEquals(Step.ENABLE_BLUETOOTH, step(adapterEnabled = false))
    }

    @Test
    fun `read starts only when permissions are granted and the adapter is on`() {
        assertEquals(Step.START_READ, step())
    }

    @Test
    fun `missing adapter never starts the read`() {
        assertEquals(Step.BLUETOOTH_UNAVAILABLE, step(adapterAvailable = false, adapterEnabled = false))
    }

    @Test
    fun `required permissions follow the Android version`() {
        assertArrayEquals(
            arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT),
            ForceClaimBluetooth.requiredPermissions(Build.VERSION_CODES.S),
        )
        assertArrayEquals(
            arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
            ForceClaimBluetooth.requiredPermissions(Build.VERSION_CODES.R),
        )
    }

    private fun step(
        permissionsGranted: Boolean = true,
        adapterAvailable: Boolean = true,
        adapterEnabled: Boolean = true,
    ) = ForceClaimBluetooth.nextStep(permissionsGranted, adapterAvailable, adapterEnabled)
}
