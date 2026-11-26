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
import java.lang.ref.WeakReference
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.TypefaceCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
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
 * While one of the app's screens is visible, the toast is drawn inside that screen's window
 * rather than as a system toast window. Its position is then the app's own: just above the
 * navigation bar, and above the keyboard only while the keyboard is open. System toast windows
 * are placed by the platform, which on some devices (e.g. Samsung tablets and tall phones) first
 * puts them where a just-closed keyboard was, mid-screen, before moving them down. Drawing it in
 * the app also keeps it visible on Android 7–9 when the app's notifications are disabled (the
 * platform suppresses toasts then, even in the foreground). If the screen changes while a toast
 * is visible, it moves to the next screen for the rest of its time, as a system toast would.
 *
 * A system toast (a custom view rebuilding the default layout, as Android 11+ ignores colours,
 * fonts and gravity of plain text toasts) is used only when no screen is available.
 *
 * Once [init] has run, a message that arrives while the app is in the background (e.g. a
 * network result) is kept and shown when the user returns, since Android 11+ drops custom
 * toasts shown from the background.
 *
 * How long a toast stays visible follows Android's accessibility "Time to take action" setting,
 * which the system also applies to toasts (Android 10+).
 */
object RuuviToast {
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var current: Toast? = null
    private var currentOverlay: View? = null
    private var overlayHost: WeakReference<Activity>? = null
    private val overlayTimeout = Runnable { removeOverlay() }
    private var currentText: CharSequence? = null
    private var currentFollowsUser = true
    private var currentMessage: String? = null
    private var hiddenAt = 0L

    private var trackedApplication: Application? = null
    private var startedActivities = 0
    private var resumedActivity: WeakReference<Activity>? = null
    private var pending: PendingToast? = null

    private class PendingToast(
        val message: CharSequence,
        val duration: ToastDuration,
        val followToNextScreen: Boolean,
    )

    internal var clock: () -> Long = SystemClock::uptimeMillis

    /** The last toast shown and how many have been shown, whichever way they were drawn (tests). */
    internal var lastShown: Pair<String, ToastDuration>? = null
        private set
    internal var shownCount = 0
        private set

    internal fun resetHistory() {
        lastShown = null
        shownCount = 0
    }

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
            resumedActivity = WeakReference(activity)
            pending?.let { toast ->
                pending = null
                show(activity, toast.message, toast.duration, toast.followToNextScreen)
                return
            }
            // A toast drawn in the previous screen follows the user to this one.
            val text = currentText
            val remaining = hiddenAt - clock()
            if (currentOverlay != null && currentFollowsUser && overlayHost?.get() !== activity &&
                text != null && remaining > 0
            ) {
                removeOverlay()
                showInActivity(activity, text, remaining)
            }
        }

        override fun onActivityPaused(activity: Activity) {
            if (resumedActivity?.get() === activity) resumedActivity = null
        }

        override fun onActivityStopped(activity: Activity) {
            startedActivities = (startedActivities - 1).coerceAtLeast(0)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    /**
     * Starts tracking the app's screens, so toasts can be drawn in the visible one, follow the
     * user between screens and wait while the app is in the background. Call once from
     * Application.onCreate.
     */
    fun init(application: Application) {
        if (trackedApplication === application) return
        stopTracking()
        trackedApplication = application
        application.registerActivityLifecycleCallbacks(lifecycleCallbacks)
    }

    /** Stops screen tracking and drops any delayed message (tests). */
    internal fun stopTracking() {
        trackedApplication?.unregisterActivityLifecycleCallbacks(lifecycleCallbacks)
        trackedApplication = null
        startedActivities = 0
        resumedActivity = null
        pending = null
    }

    private fun inBackground(appContext: Context): Boolean =
        trackedApplication != null && trackedApplication === appContext && startedActivities == 0

    /**
     * Shows [message], replacing any visible toast. With [followToNextScreen] false the toast
     * stays with the screen it was shown on instead of moving when another screen opens; use it
     * for messages about that screen only, such as a stale-measurements warning.
     */
    fun show(
        context: Context,
        message: CharSequence,
        duration: ToastDuration = ToastDuration.Short,
        followToNextScreen: Boolean = true,
    ) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(context, message, duration, followToNextScreen) }
            return
        }
        val appContext = context.applicationContext ?: context
        hideCurrent()
        if (inBackground(appContext)) {
            // Shown when an activity resumes; only the latest message is kept, as on screen.
            pending = PendingToast(message, duration, followToNextScreen)
            currentText = null
            currentMessage = null
            hiddenAt = 0L
            return
        }
        // Draw it where the user is: the resumed screen if there is one (e.g. a share result
        // arriving after the user went back to the previous screen), otherwise the caller's screen
        // if it is still on screen. The resumed screen wins even while the caller is started:
        // during navigation the new screen resumes before the old one stops, and a toast drawn on
        // the outgoing screen would not move to the new one. A toast about the caller's screen
        // only (followToNextScreen false) stays on that screen while it is on screen.
        val ownActivity = context.findActivity()
        val ownOnScreen = ownActivity?.takeIf { it.isOnScreen() }
        val visibleActivity = resumedActivity?.get()?.takeIf { it.isUsable() }
        val activity = if (followToNextScreen) visibleActivity ?: ownOnScreen else ownOnScreen ?: visibleActivity
        if (activity == null && trackedApplication === appContext) {
            // No screen to draw it in, e.g. between screens or while the only started one is
            // paused: keep it for the next one to resume rather than draw it unseen. This applies
            // to application-context callers too, since a system toast can be suppressed (Android
            // 7–9 with the app's notifications disabled) and would not be shown again on resume.
            pending = PendingToast(message, duration, followToNextScreen)
            currentText = null
            currentMessage = null
            hiddenAt = 0L
            return
        }
        val visibleMillis = recommendedTimeout(appContext, duration.visibleMillis)
        if (activity != null) {
            showInActivity(activity, message, visibleMillis)
        } else {
            showSystemToast(appContext, message, duration)
        }
        currentText = message
        currentMessage = message.toString()
        currentFollowsUser = followToNextScreen
        hiddenAt = clock() + visibleMillis
        lastShown = message.toString() to duration
        shownCount++
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
        val decor = activity.window.decorView as ViewGroup
        val baseMargin = (BOTTOM_MARGIN_DP * activity.resources.displayMetrics.density).toInt()
        val overlay = createView(activity, message).apply {
            tag = IN_APP_TOAST_TAG
            // Announced like a system toast; not clickable, so touches reach the screen below.
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val params = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
        )
        // The decor view spans the whole window, so the navigation bar and an open keyboard are
        // added to the margin; it is updated as they change so the toast tracks the keyboard.
        fun applyInsets(insets: WindowInsetsCompat?) {
            val bottom = insets?.getInsets(WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.ime())
                ?.bottom ?: 0
            if (params.bottomMargin != baseMargin + bottom) {
                params.bottomMargin = baseMargin + bottom
                overlay.layoutParams = params
            }
        }
        params.bottomMargin = baseMargin
        applyInsets(ViewCompat.getRootWindowInsets(decor))
        ViewCompat.setOnApplyWindowInsetsListener(overlay) { _, insets ->
            applyInsets(insets)
            insets
        }
        decor.addView(overlay, params)
        currentOverlay = overlay
        overlayHost = WeakReference(activity)
        mainHandler.postDelayed(overlayTimeout, visibleMillis)
    }

    private fun hideCurrent() {
        current?.cancel()
        current = null
        removeOverlay()
    }

    private fun removeOverlay() {
        mainHandler.removeCallbacks(overlayTimeout)
        currentOverlay?.let { (it.parent as? ViewGroup)?.removeView(it) }
        currentOverlay = null
        overlayHost = null
    }

    /** Whether a toast (or the toast with [message], if given) is still on screen. */
    fun isShowing(message: CharSequence? = null): Boolean =
        (current != null || currentOverlay != null) && clock() < hiddenAt &&
            (message == null || message.toString() == currentMessage)

    /** Hides the visible toast; with [message], only if that is the toast being shown. */
    fun cancel(message: CharSequence? = null) {
        if (message == null || message.toString() == pending?.message?.toString()) pending = null
        if (message != null && message.toString() != currentMessage) return
        hideCurrent()
        currentText = null
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

/** Tag of the toast view drawn inside an activity's window. */
internal const val IN_APP_TOAST_TAG = "ruuvi_in_app_toast"

private fun Activity.isUsable(): Boolean = !isFinishing && !isDestroyed

/** Started and not finishing; activities that are not lifecycle owners are assumed on screen. */
private fun Activity.isOnScreen(): Boolean =
    isUsable() && ((this as? LifecycleOwner)?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.STARTED) ?: true)

private fun Context.findActivity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }
        .filterIsInstance<Activity>()
        .firstOrNull { it.isUsable() }

fun Context.showToast(message: CharSequence, duration: ToastDuration = ToastDuration.Short) =
    RuuviToast.show(this, message, duration)

fun Context.showToast(@StringRes messageRes: Int, duration: ToastDuration = ToastDuration.Short) =
    RuuviToast.show(this, getText(messageRes), duration)
