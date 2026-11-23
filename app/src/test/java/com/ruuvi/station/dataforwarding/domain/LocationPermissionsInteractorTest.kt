package com.ruuvi.station.dataforwarding.domain

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import com.ruuvi.station.R
import com.ruuvi.station.util.ui.latestToast
import com.ruuvi.station.util.ui.latestToastText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = LocationPermissionsTestApplication::class, sdk = [Build.VERSION_CODES.S])
class LocationPermissionsInteractorTest {

    private val foregroundLauncher = mock<ActivityResultLauncher<Array<String>>>()
    private val backgroundLauncher = mock<ActivityResultLauncher<String>>()

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `foreground request does nothing before Android 12`() {
        val interactor = LocationPermissionsInteractor(createActivity())

        interactor.requestLocationPermissionApi31(foregroundLauncher, backgroundLauncher)

        verifyNoInteractions(foregroundLauncher, backgroundLauncher)
    }

    @Test
    fun `foreground request explains and launches both missing permissions`() {
        val activity = createActivity()
        val interactor = LocationPermissionsInteractor(activity)

        interactor.requestLocationPermissionApi31(foregroundLauncher, backgroundLauncher)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog)
        assertEquals(
            activity.getString(R.string.data_forwarding_background_location_message_first),
            requireNotNull(dialog.findViewById<TextView>(android.R.id.message)).text.toString(),
        )

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val permissions = argumentCaptor<Array<String>>()
        verify(foregroundLauncher).launch(permissions.capture())
        assertTrue(
            permissions.firstValue.contentEquals(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            ),
        )
        verifyNoInteractions(backgroundLauncher)
    }

    @Test
    fun `foreground request continues to background permission when foreground is granted`() {
        val activity = createActivity()
        Shadows.shadowOf(activity.application).grantPermissions(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        Shadows.shadowOf(activity.packageManager).setShouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            true,
        )
        val interactor = LocationPermissionsInteractor(activity)

        interactor.requestLocationPermissionApi31(foregroundLauncher, backgroundLauncher)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val dialog = requireNotNull(ShadowDialog.getLatestDialog() as? AlertDialog)
        assertEquals(
            activity.getString(R.string.permission_background_dialog_title),
            requireNotNull(dialog.findViewById<TextView>(androidx.appcompat.R.id.alertTitle)).text.toString(),
        )
        assertEquals(
            activity.getString(R.string.data_forwarding_background_location_message_second),
            requireNotNull(dialog.findViewById<TextView>(android.R.id.message)).text.toString(),
        )

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        verify(backgroundLauncher).launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        verifyNoInteractions(foregroundLauncher)
    }

    @Test
    fun `background request is suppressed when rationale should not be shown`() {
        val interactor = LocationPermissionsInteractor(createActivity())

        interactor.requestBackgroundLocationPermissionApi31(backgroundLauncher)

        verifyNoInteractions(backgroundLauncher)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `background request does nothing before Android 12`() {
        val activity = createActivity()
        Shadows.shadowOf(activity.packageManager).setShouldShowRequestPermissionRationale(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            true,
        )
        val interactor = LocationPermissionsInteractor(activity)

        interactor.requestBackgroundLocationPermissionApi31(backgroundLauncher)

        verifyNoInteractions(backgroundLauncher)
    }

    @Test
    fun `location toast is long and does not open settings`() {
        val activity = createActivity()
        val interactor = LocationPermissionsInteractor(activity)

        interactor.showLocationToast()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        assertEquals(
            activity.getString(R.string.data_forwarding_background_location_message_bloked),
            latestToastText(),
        )
        assertEquals(Toast.LENGTH_LONG, latestToast()?.duration)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
    }

    private fun createActivity(): ComponentActivity =
        Robolectric.buildActivity(ComponentActivity::class.java)
            .setup()
            .get()
            .apply {
                setTheme(R.style.AppTheme)
                setContentView(android.widget.FrameLayout(this))
            }
}

class LocationPermissionsTestApplication : Application()
