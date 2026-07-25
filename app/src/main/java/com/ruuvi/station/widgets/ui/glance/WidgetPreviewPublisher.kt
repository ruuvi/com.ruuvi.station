package com.ruuvi.station.widgets.ui.glance

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.collection.intSetOf
import androidx.core.content.edit
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.ruuvi.station.BuildConfig
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetProvider
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.reflect.KClass

object WidgetPreviewPublisher {
    private const val PREFERENCES_NAME = "generated_widget_previews"
    private const val PREVIEW_SCHEMA_VERSION = 5
    private const val SIMPLE_WIDGET_KEY = "simple_widget"
    private const val COMPLEX_WIDGET_KEY = "complex_widget"
    private const val APPLIED_FINGERPRINT_SUFFIX = "_applied_fingerprint"
    private const val RETRY_AFTER_SUFFIX = "_retry_after"
    private const val RETRY_FINGERPRINT_SUFFIX = "_retry_fingerprint"
    private const val RATE_LIMIT_RETRY_DELAY_MILLIS = 60L * 60L * 1_000L

    private val publishMutex = Mutex()

    suspend fun publishIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

        withContext(Dispatchers.IO) {
            publishMutex.withLock {
                publishForAndroid15(context.applicationContext)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun publishForAndroid15(context: Context) {
        val configuration = context.resources.configuration
        val fingerprint = widgetPreviewFingerprint(
            schemaVersion = PREVIEW_SCHEMA_VERSION,
            versionCode = BuildConfig.VERSION_CODE,
            packageLastUpdateTime = packageLastUpdateTime(context),
            fontScale = configuration.fontScale,
            densityDpi = configuration.densityDpi,
            nightMode = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK,
            localeTags = configuration.locales.toLanguageTags()
        )
        val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val manager = GlanceAppWidgetManager(context)

        publishProviderIfNeeded(
            context = context,
            manager = manager,
            preferences = preferences,
            providerKey = SIMPLE_WIDGET_KEY,
            receiver = SimpleWidget::class,
            fingerprint = fingerprint
        )
        publishProviderIfNeeded(
            context = context,
            manager = manager,
            preferences = preferences,
            providerKey = COMPLEX_WIDGET_KEY,
            receiver = ComplexWidgetProvider::class,
            fingerprint = fingerprint
        )
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private suspend fun publishProviderIfNeeded(
        context: Context,
        manager: GlanceAppWidgetManager,
        preferences: SharedPreferences,
        providerKey: String,
        receiver: KClass<out GlanceAppWidgetReceiver>,
        fingerprint: String
    ) {
        val appliedFingerprintKey = providerKey + APPLIED_FINGERPRINT_SUFFIX
        val frameworkPreviewIsPresent = hasGeneratedHomeScreenPreview(
            context = context,
            receiver = receiver
        )
        val needsPublishing = generatedPreviewNeedsPublishing(
            appliedFingerprint = preferences.getString(appliedFingerprintKey, null),
            currentFingerprint = fingerprint,
            frameworkPreviewIsPresent = frameworkPreviewIsPresent
        )
        if (!needsPublishing) return

        val retryAfterKey = providerKey + RETRY_AFTER_SUFFIX
        val retryFingerprintKey = providerKey + RETRY_FINGERPRINT_SUFFIX
        val now = System.currentTimeMillis()
        if (
            preferences.getString(retryFingerprintKey, null) == fingerprint &&
            preferences.getLong(retryAfterKey, 0L) > now
        ) {
            Timber.d("Generated preview update for $providerKey is waiting for the rate limit")
            return
        }

        val result = try {
            manager.setWidgetPreviews(
                receiver = receiver,
                widgetCategories = intSetOf(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Timber.e(error, "Unable to publish generated preview for $providerKey")
            return
        }

        when (result) {
            GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_SUCCESS -> {
                preferences.edit {
                    putString(appliedFingerprintKey, fingerprint)
                    remove(retryAfterKey)
                    remove(retryFingerprintKey)
                }
                Timber.d("Published generated preview for $providerKey")
            }

            GlanceAppWidgetManager.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED -> {
                preferences.edit {
                    putLong(retryAfterKey, now + RATE_LIMIT_RETRY_DELAY_MILLIS)
                    putString(retryFingerprintKey, fingerprint)
                }
                Timber.d("Generated preview update for $providerKey was rate limited")
            }

            else -> Timber.w(
                "Generated preview update for $providerKey returned unexpected result $result"
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun packageLastUpdateTime(context: Context): Long =
    context.packageManager.getPackageInfo(
        context.packageName,
        PackageManager.PackageInfoFlags.of(0)
    ).lastUpdateTime

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun hasGeneratedHomeScreenPreview(
    context: Context,
    receiver: KClass<out GlanceAppWidgetReceiver>
): Boolean {
    val receiverComponent = ComponentName(context, receiver.java)
    val generatedPreviewCategories = AppWidgetManager.getInstance(context)
        .installedProviders
        .firstOrNull { it.provider == receiverComponent }
        ?.generatedPreviewCategories
        ?: 0
    return includesHomeScreenGeneratedPreview(generatedPreviewCategories)
}

internal fun includesHomeScreenGeneratedPreview(generatedPreviewCategories: Int): Boolean =
    generatedPreviewCategories and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN != 0

internal fun generatedPreviewNeedsPublishing(
    appliedFingerprint: String?,
    currentFingerprint: String,
    frameworkPreviewIsPresent: Boolean
): Boolean = appliedFingerprint != currentFingerprint || !frameworkPreviewIsPresent

internal fun widgetPreviewFingerprint(
    schemaVersion: Int,
    versionCode: Int,
    packageLastUpdateTime: Long,
    fontScale: Float,
    densityDpi: Int,
    nightMode: Int,
    localeTags: String
): String = listOf(
    schemaVersion,
    versionCode,
    packageLastUpdateTime,
    fontScale.toRawBits(),
    densityDpi,
    nightMode,
    localeTags
).joinToString(separator = "|")
