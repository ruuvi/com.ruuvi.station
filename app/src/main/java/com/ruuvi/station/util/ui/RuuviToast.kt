package com.ruuvi.station.util.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ruuvi.station.R
import com.ruuvi.station.app.ui.theme.ToastBackground
import com.ruuvi.station.app.ui.theme.Titan

enum class ToastDuration(internal val toastLength: Int, internal val visibleMillis: Long) {
    // Matches the system's Toast.LENGTH_SHORT / LENGTH_LONG display times.
    Short(Toast.LENGTH_SHORT, 2_000L),
    Long(Toast.LENGTH_LONG, 3_500L),
}

// Layout of Android's default toast (frameworks text_toast.xml): a rounded pill sized to its
// text, with a small app icon. Only the background, font and position are the app's own.
private const val CORNER_RADIUS_DP = 28f
private const val SCREEN_MARGIN_DP = 16
private const val BOTTOM_MARGIN_DP = 16
private const val MIN_HEIGHT_DP = 48
private const val ICON_SIZE_DP = 18
private const val ICON_TEXT_SPACING_DP = 10
private const val HORIZONTAL_PADDING_DP = 16
private const val VERTICAL_PADDING_DP = 8
private const val TEXT_SIZE_SP = 14f

/**
 * Android's default toast with the app's background (light mint) and font (Mulish SemiBold,
 * Titan), shown at the bottom of the screen. It replaces the old snackbars. Showing a toast
 * replaces the one currently visible instead of queueing behind it.
 *
 * Android 11+ draws plain text toasts itself and ignores custom colours, fonts and gravity
 * for them, so this rebuilds the default toast layout as a custom toast view. Custom toast
 * views are shown while the app is in the foreground, which covers every toast in the app.
 *
 * Android 7–9 (API 24–28) suppresses all of an app's toasts while its notifications are
 * disabled, even in the foreground (Android 10 added the foreground exception). In that case
 * the same view is drawn inside the visible activity's window instead, so error and
 * confirmation messages still reach the user.
 *
 * Android 11+ also drops custom-view toasts shown while the app is in the background. Once
 * [init] has run, a message that arrives then (e.g. a network result) is kept and shown when
 * the user returns to the app.
 *
 * How long a toast counts as visible follows Android's accessibility "Time to take action"
 * setting, which the system also applies to toasts (Android 10+).
 */
object RuuviToast {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var current: Toast? = null
    private var currentOverlay: View? = null
    private val overlayTimeout = Runnable { removeOverlay() }
    private var currentMessage: String? = null
    private var hiddenAt = 0L

    private var trackedApplication: Application? = null
    private var startedActivities = 0
    private var pending: Pair<CharSequence, ToastDuration>? = null

    internal var clock: () -> Long = SystemClock::uptimeMillis

    /** Android's accessibility-adjusted display time for a toast that would last [millis]. */
    internal var recommendedTimeout: (Context, Long) -> Long = { context, millis ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(AccessibilityManager::class.java)
                ?.getRecommendedTimeoutMillis(millis.toInt(), AccessibilityManager.FLAG_CONTENT_TEXT)
                ?.toLong() ?: millis
        } else {
            millis
        }
    }

    private val lifecycleCallbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            startedActivities++
        }

        override fun onActivityResumed(activity: Activity) {
            val (message, duration) = pending ?: return
            pending = null
            show(activity, message, duration)
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities = (startedActivities - 1).coerceAtLeast(0)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    /**
     * Starts tracking whether the app is in the foreground, so toasts requested in the
     * background are delayed until the user returns. Call once from Application.onCreate.
     */
    fun init(application: Application) {
        if (trackedApplication === application) return
        trackedApplication?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        trackedApplication = application
        startedActivities = 0
        pending = null
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    /** Stops foreground tracking and drops any delayed message (tests). */
    internal fun stopTracking() {
        trackedApplication?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        trackedApplication = null
        startedActivities = 0
        pending = null
    }

    private val inBackground: Boolean
        get() = trackedApplication != null && startedActivities == 0

    fun show(context: Context, message: CharSequence, duration: ToastDuration = ToastDuration.Short) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(context, message, duration) }
            return
        }
        val appContext = context.applicationContext ?: context
        hideCurrent()
        if (inBackground) {
            // Shown when an activity resumes; only the latest message is kept, as on screen.
            pending = message to duration
            currentMessage = null
            hiddenAt = 0L
            return
        }
        val visibleMillis = recommendedTimeout(appContext, duration.visibleMillis)
        val activity = context.findActivity()
        if (activity != null && toastsSuppressed(appContext)) {
            showInActivity(activity, message, visibleMillis)
        } else {
            showSystemToast(appContext, message, duration)
        }
        currentMessage = message.toString()
        hiddenAt = clock() + visibleMillis
    }

    private fun showSystemToast(appContext: Context, message: CharSequence, duration: ToastDuration) {
        current = Toast(appContext).apply {
            @Suppress("DEPRECATION") // Custom toast views still show while the app is foreground.
            view = createView(appContext, message)
            // Replaces the system default offset, which is much higher on some devices (e.g.
            // Samsung), so the toast sits just above the navigation bar.
            val bottomMargin = (BOTTOM_MARGIN_DP * appContext.resources.displayMetrics.density).toInt()
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, bottomMargin)
            this.duration = duration.toastLength
            show()
        }
    }

    private fun showInActivity(activity: Activity, message: CharSequence, visibleMillis: Long) {
        // The decor view spans the whole window, so the navigation bar (and keyboard) inset is
        // added to the margin to keep the toast where a system toast would sit.
        val decor = activity.window.decorView as ViewGroup
        val insets = ViewCompat.getRootWindowInsets(decor)
            ?.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
        val bottomMargin = (BOTTOM_MARGIN_DP * activity.resources.displayMetrics.density).toInt() +
            (insets?.bottom ?: 0)
        val overlay = createView(activity, message).apply {
            tag = IN_APP_TOAST_TAG
            // Announced like a system toast; not clickable, so touches reach the screen below.
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        decor.addView(
            overlay,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply { this.bottomMargin = bottomMargin },
        )
        currentOverlay = overlay
        mainHandler.postDelayed(overlayTimeout, visibleMillis)
    }

    private fun toastsSuppressed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()

    private fun hideCurrent() {
        current?.cancel()
        current = null
        removeOverlay()
    }

    private fun removeOverlay() {
        mainHandler.removeCallbacks(overlayTimeout)
        currentOverlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentOverlay = null
    }

    /** Whether a toast (or the toast with [message], if given) is still on screen. */
    fun isShowing(message: CharSequence? = null): Boolean =
        (current != null || currentOverlay != null) && clock() < hiddenAt &&
            (message == null || message.toString() == currentMessage)

    /** Hides the visible toast; with [message], only if that is the toast being shown. */
    fun cancel(message: CharSequence? = null) {
        if (message == null || message.toString() == pending?.first?.toString()) pending = null
        if (message != null && message.toString() != currentMessage) return
        hideCurrent()
        currentMessage = null
        hiddenAt = 0L
    }

    private fun createView(context: Context, message: CharSequence): View {
        val density = context.resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        // The app icon, as Android 12+ shows on default toasts.
        val icon = ImageView(context).apply {
            setImageDrawable(context.packageManager.getApplicationIcon(context.applicationInfo))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val text = TextView(context).apply {
            this.text = message
            setTextColor(Titan.toArgb())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, TEXT_SIZE_SP)
            ResourcesCompat.getFont(context, R.font.mulish_regular)?.let { font ->
                typeface = TypefaceCompat.create(context, font, 600, false)
            }
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(MIN_HEIGHT_DP)
            setPadding(
                dp(HORIZONTAL_PADDING_DP),
                dp(VERTICAL_PADDING_DP),
                dp(HORIZONTAL_PADDING_DP),
                dp(VERTICAL_PADDING_DP),
            )
            background = GradientDrawable().apply {
                setColor(ToastBackground.toArgb())
                cornerRadius = CORNER_RADIUS_DP * density
            }
            addView(
                icon,
                LinearLayout.LayoutParams(dp(ICON_SIZE_DP), dp(ICON_SIZE_DP)).apply {
                    marginEnd = dp(ICON_TEXT_SPACING_DP)
                },
            )
            addView(
                text,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }
        // Keeps a long message's pill off the screen edges.
        return FrameLayout(context).apply {
            setPadding(dp(SCREEN_MARGIN_DP), 0, dp(SCREEN_MARGIN_DP), 0)
            addView(
                content,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_HORIZONTAL,
                ),
            )
        }
    }
}

/** Tag of the in-app toast view drawn on API 24–28 when the app's notifications are disabled. */
internal const val IN_APP_TOAST_TAG = "ruuvi_in_app_toast"

private fun Context.findActivity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull { !it.isFinishing }

fun Context.showToast(message: CharSequence, duration: ToastDuration = ToastDuration.Short) =
    RuuviToast.show(this, message, duration)

fun Context.showToast(@StringRes messageRes: Int, duration: ToastDuration = ToastDuration.Short) =
    RuuviToast.show(this, getText(messageRes), duration)
