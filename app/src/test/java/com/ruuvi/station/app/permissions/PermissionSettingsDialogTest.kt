package com.ruuvi.station.app.permissions

import android.app.Activity
import android.app.Application
import android.content.DialogInterface
import android.net.Uri
import android.os.Looper
import android.provider.Settings
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(application = PermissionSettingsDialogTestApplication::class, sdk = [35])
class PermissionSettingsDialogTest {

    @Test
    fun `dialog shows the permission message with Cancel and Open settings actions`() {
        val activity = createActivity()

        activity.showPermissionSettingsDialog(R.string.notification_permission_needed)
        val dialog = latestPermissionSettingsDialog()

        assertTrue(dialog.isShowing)
        assertEquals(activity.getString(R.string.notification_permission_needed), dialog.messageText())
        assertEquals(activity.getString(R.string.open_settings), dialog.getButton(DialogInterface.BUTTON_POSITIVE).text.toString())
        assertEquals(activity.getString(R.string.cancel), dialog.getButton(DialogInterface.BUTTON_NEGATIVE).text.toString())
    }

    @Test
    fun `Open settings opens this app's system settings page`() {
        val activity = createActivity()

        activity.showPermissionSettingsDialog(R.string.permission_location_needed_api31)
        val dialog = latestPermissionSettingsDialog()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val intent = Shadows.shadowOf(activity).nextStartedActivity
        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals(Uri.fromParts("package", activity.packageName, null), intent.data)
        assertFalse(dialog.isShowing)
    }

    @Test
    fun `Cancel closes the dialog without opening settings`() {
        val activity = createActivity()

        activity.showPermissionSettingsDialog(R.string.permission_location_needed_api31)
        val dialog = latestPermissionSettingsDialog()
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
        assertFalse(dialog.isShowing)
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

internal fun latestPermissionSettingsDialog(): AlertDialog =
    ShadowDialog.getLatestDialog() as AlertDialog

internal fun AlertDialog.messageText(): String? =
    findViewById<TextView>(android.R.id.message)?.text?.toString()

class PermissionSettingsDialogTestApplication : Application()
