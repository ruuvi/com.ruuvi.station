package com.ruuvi.station.widgets.update

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.res.Configuration
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.google.gson.Gson
import com.ruuvi.station.widgets.domain.ComplexWidgetPreferencesInteractor
import com.ruuvi.station.widgets.domain.WidgetInteractor
import com.ruuvi.station.widgets.domain.WidgetPreferencesInteractor
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetGlanceWidget
import com.ruuvi.station.widgets.ui.complexWidget.ComplexWidgetPrefKeys
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetGlanceWidget
import com.ruuvi.station.widgets.ui.simpleWidget.SimpleWidgetPrefKeys
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class WidgetUpdater(
    private val simplePreferences: WidgetPreferencesInteractor,
    private val complexPreferences: ComplexWidgetPreferencesInteractor,
    private val widgetInteractor: WidgetInteractor,
) {
    private val gson = Gson()
    private val simpleWidgetLocks = List(WIDGET_LOCK_STRIPE_COUNT) { Mutex() }
    private val complexWidgetLocks = List(WIDGET_LOCK_STRIPE_COUNT) { Mutex() }

    suspend fun updateSimpleWidget(context: Context, appWidgetId: Int) =
        withContext(Dispatchers.IO) {
            simpleWidgetLocks.lockFor(appWidgetId).withLock {
                val applicationContext = context.applicationContext
                val sensorId = simplePreferences.getSimpleWidgetSensor(appWidgetId)
                val widgetType = simplePreferences.getSimpleWidgetType(appWidgetId)

                if (!sensorId.isNullOrEmpty()) {
                    val widgetData = widgetInteractor.getSimpleWidgetData(
                        sensorId = sensorId,
                        widgetType = widgetType,
                    )
                    val glanceId = getGlanceIdOrNull(
                        context = applicationContext,
                        appWidgetId = appWidgetId,
                        widgetName = "Simple",
                    ) ?: return@withLock
                    val widgetOptions = AppWidgetManager.getInstance(applicationContext)
                        .getAppWidgetOptions(appWidgetId)
                    val observedSize = resolveSimpleWidgetHostSize(
                        minimumWidth =
                            widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH),
                        minimumHeight =
                            widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                        maximumWidth =
                            widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH),
                        maximumHeight =
                            widgetOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT),
                        isLandscape =
                            applicationContext.resources.configuration.orientation ==
                                Configuration.ORIENTATION_LANDSCAPE,
                    )
                    val isTablet =
                        applicationContext.resources.configuration.smallestScreenWidthDp >=
                            TABLET_MINIMUM_WIDTH_DP
                    val currentOrientation =
                        applicationContext.resources.configuration.orientation

                    updateAppWidgetState(applicationContext, glanceId) { preferences ->
                        preferences[SimpleWidgetPrefKeys.sensorId] = sensorId
                        preferences[SimpleWidgetPrefKeys.displayName] =
                            widgetData?.displayName.orEmpty()
                        preferences[SimpleWidgetPrefKeys.sensorValue] =
                            widgetData?.sensorValue.orEmpty()
                        preferences[SimpleWidgetPrefKeys.unit] =
                            widgetData?.unit.orEmpty()
                        preferences[SimpleWidgetPrefKeys.measurementName] =
                            widgetData?.measurementName.orEmpty()
                        preferences[SimpleWidgetPrefKeys.updated] =
                            widgetData?.updated.orEmpty()
                        preferences[SimpleWidgetPrefKeys.measurementType] =
                            widgetType.code.toString()
                        val hasValidCapturedSize =
                            preferences[SimpleWidgetPrefKeys.typographySizeObserved] == true &&
                                isSimpleWidgetTwoByOneSize(
                                    width = preferences[SimpleWidgetPrefKeys.typographyWidth],
                                    height = preferences[SimpleWidgetPrefKeys.typographyHeight],
                                    isTablet = isTablet,
                                )
                        val observedSizeIsTwoByOne = isSimpleWidgetTwoByOneSize(
                            width = observedSize.width,
                            height = observedSize.height,
                            isTablet = isTablet,
                        )
                        val shouldMigrateCapturedSize =
                            shouldMigrateSimpleWidgetTypographySize(
                                hasValidCapturedSize = hasValidCapturedSize,
                                captureVersion =
                                    preferences[SimpleWidgetPrefKeys.typographySizeVersion],
                                capturedOrientation =
                                    preferences[SimpleWidgetPrefKeys.typographyOrientation],
                                currentOrientation = currentOrientation,
                                isTablet = isTablet,
                                observedSizeIsTwoByOne = observedSizeIsTwoByOne,
                            )
                        val hasCapturedTwoByOneSize =
                            hasValidCapturedSize && !shouldMigrateCapturedSize
                        if (!hasCapturedTwoByOneSize) {
                            preferences[SimpleWidgetPrefKeys.typographySizeObserved] = false
                            preferences.remove(SimpleWidgetPrefKeys.typographyWidth)
                            preferences.remove(SimpleWidgetPrefKeys.typographyHeight)
                            preferences.remove(SimpleWidgetPrefKeys.typographySizeVersion)
                            preferences.remove(SimpleWidgetPrefKeys.typographyOrientation)
                        }
                        if (
                            shouldCaptureSimpleWidgetTypographySize(
                                alreadyCaptured = hasCapturedTwoByOneSize,
                                width = observedSize.width,
                                height = observedSize.height,
                                isTablet = isTablet,
                            )
                        ) {
                            preferences[SimpleWidgetPrefKeys.typographySizeObserved] = true
                            preferences[SimpleWidgetPrefKeys.typographyWidth] = observedSize.width
                            preferences[SimpleWidgetPrefKeys.typographyHeight] = observedSize.height
                            preferences[SimpleWidgetPrefKeys.typographySizeVersion] =
                                SIMPLE_WIDGET_TYPOGRAPHY_SIZE_VERSION
                            preferences[SimpleWidgetPrefKeys.typographyOrientation] =
                                currentOrientation
                        } else if (hasValidCapturedSize) {
                            preferences[SimpleWidgetPrefKeys.typographySizeVersion] =
                                SIMPLE_WIDGET_TYPOGRAPHY_SIZE_VERSION
                            if (!isTablet) {
                                preferences[SimpleWidgetPrefKeys.typographyOrientation] =
                                    currentOrientation
                            }
                        }
                    }

                    SimpleWidgetGlanceWidget.update(applicationContext, glanceId)
                }
            }
        }

    suspend fun updateComplexWidget(context: Context, appWidgetId: Int) =
        withContext(Dispatchers.IO) {
            complexWidgetLocks.lockFor(appWidgetId).withLock {
                val applicationContext = context.applicationContext
                val widgetSettings = complexPreferences.getComplexWidgetSettings(appWidgetId)
                val cloudSensors = widgetInteractor.getCloudSensorsList()
                    .filter { cloudSensor ->
                        widgetSettings.any { setting -> setting.sensorId == cloudSensor.id }
                    }
                val sensorsData = cloudSensors.map { sensor ->
                    val settings = widgetSettings.firstOrNull { it.sensorId == sensor.id }
                    widgetInteractor.getComplexWidgetData(sensor.id, settings)
                }
                val glanceId = getGlanceIdOrNull(
                    context = applicationContext,
                    appWidgetId = appWidgetId,
                    widgetName = "Complex",
                ) ?: return@withLock

                updateAppWidgetState(applicationContext, glanceId) { preferences ->
                    preferences[ComplexWidgetPrefKeys.data] = gson.toJson(sensorsData)
                }
                ComplexWidgetGlanceWidget.update(applicationContext, glanceId)
            }
        }

    suspend fun updateSimpleWidgets(context: Context, appWidgetIds: IntArray) {
        updateWidgetsSequentially(
            appWidgetIds = appWidgetIds,
            update = { appWidgetId -> updateSimpleWidget(context, appWidgetId) },
            onFailure = { appWidgetId, error ->
                Timber.e(error, "Simple widget update failed for Id $appWidgetId")
            },
        )
    }

    suspend fun updateComplexWidgets(context: Context, appWidgetIds: IntArray) {
        updateWidgetsSequentially(
            appWidgetIds = appWidgetIds,
            update = { appWidgetId -> updateComplexWidget(context, appWidgetId) },
            onFailure = { appWidgetId, error ->
                Timber.e(error, "Complex widget update failed for Id $appWidgetId")
            },
        )
    }

    private suspend fun getGlanceIdOrNull(
        context: Context,
        appWidgetId: Int,
        widgetName: String,
    ) = try {
        GlanceAppWidgetManager(context).getGlanceIdBy(appWidgetId)
    } catch (error: IllegalArgumentException) {
        Timber.d("$widgetName widget $appWidgetId was deleted before its update")
        null
    }

    private fun List<Mutex>.lockFor(appWidgetId: Int): Mutex =
        this[widgetLockStripeIndex(appWidgetId, size)]

    private companion object {
        const val WIDGET_LOCK_STRIPE_COUNT = 32
    }
}

internal fun shouldCaptureSimpleWidgetTypographySize(
    alreadyCaptured: Boolean,
    width: Int,
    height: Int,
    isTablet: Boolean = false,
): Boolean =
    !alreadyCaptured && isSimpleWidgetTwoByOneSize(width, height, isTablet)

internal fun shouldMigrateSimpleWidgetTypographySize(
    hasValidCapturedSize: Boolean,
    captureVersion: Int?,
    capturedOrientation: Int?,
    currentOrientation: Int,
    isTablet: Boolean,
    observedSizeIsTwoByOne: Boolean,
): Boolean =
    hasValidCapturedSize &&
        isTablet &&
        observedSizeIsTwoByOne &&
        (
            captureVersion != SIMPLE_WIDGET_TYPOGRAPHY_SIZE_VERSION ||
                capturedOrientation != currentOrientation
        )

internal data class SimpleWidgetHostSize(val width: Int, val height: Int)

internal fun resolveSimpleWidgetHostSize(
    minimumWidth: Int,
    minimumHeight: Int,
    maximumWidth: Int,
    maximumHeight: Int,
    isLandscape: Boolean,
): SimpleWidgetHostSize {
    val width = if (isLandscape) {
        maximumWidth.takeIf { it > 0 } ?: minimumWidth
    } else {
        minimumWidth.takeIf { it > 0 } ?: maximumWidth
    }
    val height = if (isLandscape) {
        minimumHeight.takeIf { it > 0 } ?: maximumHeight
    } else {
        maximumHeight.takeIf { it > 0 } ?: minimumHeight
    }
    return SimpleWidgetHostSize(width, height)
}

internal fun isSimpleWidgetTwoByOneSize(
    width: Int?,
    height: Int?,
    isTablet: Boolean = false,
): Boolean {
    if (width == null || height == null || width <= 0 || height <= 0) return false

    return if (isTablet) {
        val aspectRatio = width.toFloat() / height
        height in SIMPLE_WIDGET_ONE_ROW_MIN_HEIGHT_DP until
            TABLET_SIMPLE_WIDGET_TWO_ROW_MIN_HEIGHT_DP &&
            aspectRatio >= TABLET_TWO_BY_ONE_MIN_ASPECT_RATIO &&
            aspectRatio <= TABLET_TWO_BY_ONE_MAX_ASPECT_RATIO
    } else {
        width in SIMPLE_WIDGET_TWO_COLUMN_MIN_WIDTH_DP until
            SIMPLE_WIDGET_THREE_COLUMN_MIN_WIDTH_DP &&
            height in SIMPLE_WIDGET_ONE_ROW_MIN_HEIGHT_DP until
            SIMPLE_WIDGET_TWO_ROW_MIN_HEIGHT_DP
    }
}

private const val SIMPLE_WIDGET_TWO_COLUMN_MIN_WIDTH_DP = 110
private const val SIMPLE_WIDGET_THREE_COLUMN_MIN_WIDTH_DP = 220
private const val SIMPLE_WIDGET_ONE_ROW_MIN_HEIGHT_DP = 40
private const val SIMPLE_WIDGET_TWO_ROW_MIN_HEIGHT_DP = 120
private const val TABLET_MINIMUM_WIDTH_DP = 600
private const val TABLET_SIMPLE_WIDGET_TWO_ROW_MIN_HEIGHT_DP = 220
private const val TABLET_TWO_BY_ONE_MIN_ASPECT_RATIO = 1.5f
private const val TABLET_TWO_BY_ONE_MAX_ASPECT_RATIO = 3.25f
private const val SIMPLE_WIDGET_TYPOGRAPHY_SIZE_VERSION = 2

internal fun widgetLockStripeIndex(appWidgetId: Int, stripeCount: Int): Int {
    require(stripeCount > 0) { "Widget lock stripe count must be positive" }
    return Math.floorMod(appWidgetId, stripeCount)
}

internal suspend fun updateWidgetsSequentially(
    appWidgetIds: IntArray,
    update: suspend (appWidgetId: Int) -> Unit,
    onFailure: (appWidgetId: Int, error: Exception) -> Unit,
) {
    for (appWidgetId in appWidgetIds) {
        try {
            update(appWidgetId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            onFailure(appWidgetId, error)
        }
    }
}
