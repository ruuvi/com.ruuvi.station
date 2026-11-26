package com.ruuvi.station.app.permissions

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.os.Build
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = PermissionsTestApplication::class, sdk = [Build.VERSION_CODES.S])
class PermissionsInteractorTest {

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `getPermissionsMissedMessage returns location_needed on API 30`() {
        assertEquals(R.string.permission_location_needed, PermissionsInteractor.getPermissionsMissedMessage())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `getPermissionsMissedMessage returns location_needed_api31 on API 31`() {
        assertEquals(R.string.permission_location_needed_api31, PermissionsInteractor.getPermissionsMissedMessage())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.R])
    fun `getPermissionRequestMessage returns request_message on API 30`() {
        assertEquals(R.string.permission_dialog_request_message, PermissionsInteractor.getPermissionRequestMessage())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `getPermissionRequestMessage returns request_message_api31 on API 31`() {
        assertEquals(R.string.permission_dialog_request_message_api31, PermissionsInteractor.getPermissionRequestMessage())
    }

    @Test
    fun `arePermissionsGranted returns false when permissions missing`() {
        val activity = createActivity()
        val interactor = PermissionsInteractor(activity)

        assertFalse(interactor.arePermissionsGranted())
    }

    @Test
    fun `arePermissionsGranted returns true when required Bluetooth permissions granted on API 31`() {
        val activity = createActivity()
        Shadows.shadowOf(activity.application).grantPermissions(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        )
        val interactor = PermissionsInteractor(activity)

        assertTrue(interactor.arePermissionsGranted())
    }

    @Test
    fun `showPermissionSettingsDialog offers a route to app settings`() {
        val activity = createActivity()
        val interactor = PermissionsInteractor(activity)

        interactor.showPermissionSettingsDialog()

        val dialog = latestPermissionSettingsDialog()
        assertTrue(dialog.isShowing)
        assertEquals(activity.getString(R.string.permission_location_needed_api31), dialog.messageText())
        assertEquals(activity.getString(R.string.open_settings), dialog.getButton(DialogInterface.BUTTON_POSITIVE).text.toString())
    }

    private fun createActivity(): Activity =
        Robolectric.buildActivity(AppCompatActivity::class.java)
            .setup()
            .get()
            .apply {
                setTheme(R.style.AppTheme)
                setContentView(FrameLayout(this))
            }
}

class PermissionsTestApplication : Application()
