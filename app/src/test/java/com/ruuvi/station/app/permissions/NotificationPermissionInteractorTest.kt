package com.ruuvi.station.app.permissions

import android.Manifest
import android.app.Application
import android.content.DialogInterface
import android.os.Build
import android.os.Looper
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = NotificationPermissionsTestApplication::class, sdk = [Build.VERSION_CODES.TIRAMISU])
class NotificationPermissionInteractorTest {

    @Test
    fun `onPermissionDenied offers a route to app settings`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java).create()
        val activity = controller.get().apply {
            setTheme(R.style.AppTheme)
            setContentView(FrameLayout(this))
        }
        val interactor = NotificationPermissionInteractor(activity)
        controller.start().resume().visible()

        interactor.onPermissionDenied()

        val dialog = latestPermissionSettingsDialog()
        assertTrue(dialog.isShowing)
        assertEquals(activity.getString(R.string.notification_permission_needed), dialog.messageText())
        assertEquals(activity.getString(R.string.open_settings), dialog.getButton(DialogInterface.BUTTON_POSITIVE).text.toString())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S])
    fun `checkAndRequest on pre-Tiramisu does not check or request notification permission`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java).create()
        val activity = controller.get().apply {
            setTheme(R.style.AppTheme)
            setContentView(FrameLayout(this))
        }
        val interactor = NotificationPermissionInteractor(activity)
        controller.start().resume().visible()

        interactor.checkAndRequest()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `checkAndRequest when permission granted does not launch request`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java).create()
        val activity = controller.get().apply {
            setTheme(R.style.AppTheme)
            setContentView(FrameLayout(this))
        }
        Shadows.shadowOf(activity.application).grantPermissions(
            Manifest.permission.POST_NOTIFICATIONS,
        )
        val interactor = NotificationPermissionInteractor(activity)
        controller.start().resume().visible()

        interactor.checkAndRequest()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }
}

class NotificationPermissionsTestApplication : Application()
