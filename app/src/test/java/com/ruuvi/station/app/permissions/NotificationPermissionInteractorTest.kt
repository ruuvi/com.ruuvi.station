package com.ruuvi.station.app.permissions

import android.Manifest
import android.app.Application
import android.os.Build
import android.os.Looper
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ruuvi.station.R
import com.ruuvi.station.util.ui.latestToast
import com.ruuvi.station.util.ui.latestToastText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(application = NotificationPermissionsTestApplication::class, sdk = [Build.VERSION_CODES.TIRAMISU])
class NotificationPermissionInteractorTest {

    @Test
    fun `onPermissionDenied shows a long permission toast without opening settings`() {
        val controller = Robolectric.buildActivity(AppCompatActivity::class.java).create()
        val activity = controller.get().apply {
            setTheme(R.style.AppTheme)
            setContentView(FrameLayout(this))
        }
        val interactor = NotificationPermissionInteractor(activity)
        controller.start().resume().visible()

        interactor.onPermissionDenied()
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(100, TimeUnit.MILLISECONDS)

        assertEquals(
            activity.getString(R.string.notification_permission_needed),
            latestToastText(),
        )
        assertEquals(Toast.LENGTH_LONG, latestToast()?.duration)
        assertNull(Shadows.shadowOf(activity).nextStartedActivity)
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
