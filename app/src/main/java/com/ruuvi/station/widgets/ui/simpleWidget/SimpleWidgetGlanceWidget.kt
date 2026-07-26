package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import androidx.datastore.preferences.core.Preferences
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.PreviewSizeMode
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import com.ruuvi.station.dashboard.ui.DashboardActivity
import com.ruuvi.station.tagdetails.ui.SensorCardActivity
import com.ruuvi.station.R
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.widgets.data.WidgetType
import com.ruuvi.station.widgets.ui.glance.CustomFontText
import com.ruuvi.station.widgets.ui.glance.GlanceColors
import com.ruuvi.station.widgets.ui.glance.GlanceFontUtils
import com.ruuvi.station.widgets.ui.glance.RefreshButton
import com.ruuvi.station.widgets.ui.glance.WidgetRefreshButtonDefaults
import com.ruuvi.station.widgets.ui.glance.getZoomFactor
import com.ruuvi.station.widgets.ui.glance.scaledBy
import com.ruuvi.station.widgets.ui.glance.toWidgetSp
import com.ruuvi.station.widgets.update.WidgetRefreshScheduler
import com.ruuvi.station.widgets.update.resolveAppWidgetId
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import timber.log.Timber

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override val previewSizeMode: PreviewSizeMode =
        SizeMode.Responsive(setOf(DpSize(180.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()

            val sensorId = prefs[SimpleWidgetPrefKeys.sensorId]
            val displayName = prefs[SimpleWidgetPrefKeys.displayName]
            val sensorValue = prefs[SimpleWidgetPrefKeys.sensorValue]
            val unit = prefs[SimpleWidgetPrefKeys.unit]
            val measurementName = prefs[SimpleWidgetPrefKeys.measurementName]
            val updated = prefs[SimpleWidgetPrefKeys.updated]
            val measurementTypeCode = prefs[SimpleWidgetPrefKeys.measurementType]

            SimpleWidgetContent(
                sensorId = sensorId,
                displayName = displayName?.takeIf { it.isNotBlank() } ?: "-",
                sensorValue = sensorValue?.takeIf { it.isNotBlank() } ?: "-",
                unit = unit.orEmpty(),
                measurementName = measurementName?.takeIf { it.isNotBlank() } ?: "-",
                updated = updated?.takeIf { it.isNotBlank() } ?: "-",
                measurementType = measurementTypeCode?.toIntOrNull()?.let { WidgetType.getByCode(it) },
                renderingMode = SimpleWidgetRenderingMode.RUNTIME,
            )
        }
    }

    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ?: Locale.getDefault()
        val previewValue = NumberFormat.getNumberInstance(locale).run {
            minimumFractionDigits = 1
            maximumFractionDigits = 1
            format(21.5)
        }

        provideContent {
            SimpleWidgetContent(
                sensorId = null,
                displayName = context.getString(R.string.widget_preview_sensor_name),
                sensorValue = previewValue,
                unit = context.getString(WidgetType.TEMPERATURE.unitType.unit),
                measurementName = context.getString(R.string.temperature),
                updated = "12:34",
                measurementType = WidgetType.TEMPERATURE,
                renderingMode = SimpleWidgetRenderingMode.GENERATED_PREVIEW,
                typographyScale = GENERATED_PREVIEW_TYPOGRAPHY_SCALE,
            )
        }
    }

    internal suspend fun rerenderAllFromState(context: Context) {
        val applicationContext = context.applicationContext
        val glanceIds = GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(SimpleWidgetGlanceWidget::class.java)

        for (glanceId in glanceIds) {
            try {
                update(applicationContext, glanceId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                Timber.e(error, "Unable to rerender simple widget $glanceId")
            }
        }
    }
}

class RefreshSimpleWidgetAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val appWidgetId = resolveAppWidgetId(context, glanceId, "Simple") ?: return
        WidgetRefreshScheduler.enqueueSimpleRefresh(context, appWidgetId)
    }
}

class OpenSimpleWidgetSensorAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val sensorId = SimpleWidget.sensorIdFromParameters(parameters) ?: return
        val appWidgetId = resolveAppWidgetId(context, glanceId, "Simple") ?: return
        SensorCardActivity.createWidgetPendingIntent(context, sensorId, appWidgetId)?.send()
    }
}

@Composable
private fun SimpleWidgetContent(
    sensorId: String?,
    displayName: String,
    sensorValue: String,
    unit: String,
    measurementName: String,
    updated: String,
    measurementType: WidgetType?,
    renderingMode: SimpleWidgetRenderingMode,
    typographyScale: Float = 1f,
) {
    val openAction = if (!sensorId.isNullOrEmpty()) {
        actionRunCallback<OpenSimpleWidgetSensorAction>(
            SimpleWidget.openSensorActionParameters(sensorId)
        )
    } else {
        actionStartActivity<DashboardActivity>()
    }

    val context = LocalContext.current
    val embeddedColors = resolveSimpleWidgetEmbeddedColors(
        renderingMode = renderingMode,
        valueColor = { GlanceColors.resolvedValueColor(context) },
        secondaryColor = { GlanceColors.resolvedWidgetSensorNameColor(context) },
    )
    val zoomFactor = getZoomFactor(context)
    val size = LocalSize.current
    val widgetWidth = validDimensionOrFallback(size.width, 110.dp)
    val widgetHeight = validDimensionOrFallback(size.height, 45.dp)
    val baseConfig = SimpleWidgetLayoutConfig.fromHeight(
        height = widgetHeight,
        zoomFactor = zoomFactor
    )
    val config = baseConfig.copy(
        displayNameFontSize = baseConfig.displayNameFontSize.scaledBy(typographyScale),
        valueFontSize = baseConfig.valueFontSize.scaledBy(typographyScale),
        secondaryFontSize = baseConfig.secondaryFontSize.scaledBy(typographyScale)
    )
    val displayNameFontSize = config.displayNameFontSize.toWidgetSp(context)
    val valueFontSize = config.valueFontSize.toWidgetSp(context)
    val secondaryFontSize = config.secondaryFontSize.toWidgetSp(context)
    val secondaryFontHeight = GlanceFontUtils.measureSystemFontHeight(
        context = context,
        fontSize = secondaryFontSize,
        bold = true
    )
    val visibility = calculateSimpleWidgetContentVisibility(
        widgetHeight = widgetHeight,
        config = config,
        textHeights = SimpleWidgetTextHeights(
            displayName = GlanceFontUtils.measureSystemFontHeight(
                context = context,
                fontSize = displayNameFontSize,
                bold = true
            ),
            value = GlanceFontUtils.measureCustomFontHeight(
                context,
                valueFontSize,
                R.font.oswald_bold
            ),
            secondary = secondaryFontHeight
        ),
        hasUnit = unit.isNotBlank(),
        isAirQuality = measurementType == WidgetType.AIR_QUALITY
    )

    val availableWidth = positiveDp(widgetWidth - (config.horizontalPadding * 2))
    val timestampMaxWidth = calculateTimestampMaxWidth(
        widgetWidth = widgetWidth,
        contentStartPadding = config.horizontalPadding,
        refreshEndInset = WidgetRefreshButtonDefaults.backingEndInset,
        inlineGap = config.inlineSpacing
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceColors.background)
            .clickable(openAction)
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(
                    horizontal = config.horizontalPadding,
                    vertical = config.verticalPadding
                )
        ) {
            if (visibility.showDisplayName) {
                Text(
                    text = displayName,
                    modifier = GlanceModifier.fillMaxWidth(),
                    style = TextStyle(
                        color = GlanceColors.widgetSensorName,
                        fontSize = displayNameFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName,
                    config = config,
                    availableWidth = availableWidth,
                    visibility = visibility,
                    embeddedValueColor = embeddedColors.value,
                )
            } else {
                GlanceMeasurementDisplay(
                    sensorValue = sensorValue,
                    unit = unit,
                    measurementName = measurementName,
                    config = config,
                    availableWidth = availableWidth,
                    showMeasurementDescription = visibility.showMeasurementDescription,
                    embeddedValueColor = embeddedColors.value,
                    embeddedUnitColor = embeddedColors.secondary,
                )
            }

            if (visibility.showTimestamp) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = updated,
                        modifier = GlanceModifier.width(timestampMaxWidth),
                        style = TextStyle(
                            color = GlanceColors.widgetSensorName,
                            fontSize = secondaryFontSize
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        RefreshButton(
            backingColor = GlanceColors.background,
            action = actionRunCallback<RefreshSimpleWidgetAction>()
        )
    }
}

internal enum class SimpleWidgetRenderingMode {
    RUNTIME,
    GENERATED_PREVIEW,
}

internal data class SimpleWidgetEmbeddedColors(
    val value: Int?,
    val secondary: Int?,
)

internal fun resolveSimpleWidgetEmbeddedColors(
    renderingMode: SimpleWidgetRenderingMode,
    valueColor: () -> Int,
    secondaryColor: () -> Int,
): SimpleWidgetEmbeddedColors =
    when (renderingMode) {
        SimpleWidgetRenderingMode.RUNTIME -> SimpleWidgetEmbeddedColors(
            value = null,
            secondary = null,
        )
        SimpleWidgetRenderingMode.GENERATED_PREVIEW -> SimpleWidgetEmbeddedColors(
            value = valueColor(),
            secondary = secondaryColor(),
        )
    }

@Composable
private fun GlanceMeasurementDisplay(
    sensorValue: String,
    unit: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp,
    showMeasurementDescription: Boolean,
    embeddedValueColor: Int?,
    embeddedUnitColor: Int?
) {
    val context = LocalContext.current
    val rowWidth = positiveDp(availableWidth - config.inlineSpacing)
    val valueWidthFraction = if (unit.isBlank()) 1f else 0.7f
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = sensorValue,
            fontSize = config.valueFontSize.toWidgetSp(context),
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold,
            maxWidth = positiveDp(rowWidth * valueWidthFraction),
            embeddedColor = embeddedValueColor
        )

        if (unit.isNotBlank()) {
            Spacer(modifier = GlanceModifier.width(config.inlineSpacing))
            CustomFontText(
                text = unit,
                fontSize = config.secondaryFontSize.toWidgetSp(context),
                colorProvider = GlanceColors.widgetSensorName,
                fontResId = R.font.oswald_regular,
                modifier = GlanceModifier
                    .padding(top = config.unitPadding),
                maxWidth = positiveDp(rowWidth * (1f - valueWidthFraction)),
                embeddedColor = embeddedUnitColor
            )
        }
    }

    if (showMeasurementDescription) {
        Text(
            text = measurementName,
            modifier = GlanceModifier.fillMaxWidth(),
            style = TextStyle(
                color = GlanceColors.widgetSensorName,
                fontSize = config.secondaryFontSize.toWidgetSp(context)
            ),
            maxLines = 1
        )
    }
}

private const val GENERATED_PREVIEW_TYPOGRAPHY_SCALE = 0.9f

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp,
    visibility: SimpleWidgetContentVisibility,
    embeddedValueColor: Int?
) {
    val context = LocalContext.current
    val aqiText = sensorValue.substringBefore("/")
    val aqiValue = aqiText.toDoubleOrNull()?.takeIf { it.isFinite() }
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = ColorProvider(day = aqiColor, night = aqiColor)

    val progressBarWidth = positiveDp(
        availableWidth -
            WidgetRefreshButtonDefaults.touchTargetSize -
            (config.inlineSpacing * 2)
    )

    Column {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomFontText(
                text = aqiText,
                fontSize = config.valueFontSize.toWidgetSp(context),
                colorProvider = GlanceColors.valueColor,
                fontResId = R.font.oswald_bold,
                maxWidth = positiveDp(availableWidth * 0.55f),
                embeddedColor = embeddedValueColor
            )

            Spacer(modifier = GlanceModifier.width(config.inlineSpacing))

            Box(
                modifier = GlanceModifier
                    .width(positiveDp(availableWidth * 0.4f))
                    .height(visibility.aqiInfoHeight)
            ) {
                Text(
                    text = "/100",
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = config.unitPadding),
                    style = TextStyle(
                        color = GlanceColors.valueColor,
                        fontSize = config.secondaryFontSize.toWidgetSp(context),
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )

                if (visibility.showMeasurementDescription) {
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            text = measurementName,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(bottom = config.aqiMeasurementPadding),
                            style = TextStyle(
                                color = GlanceColors.widgetSensorName,
                                fontSize = config.secondaryFontSize.toWidgetSp(context)
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (visibility.showAqiProgress) {
            GlanceProgressBarWithDot(
                progress = (aqiValue?.toFloat() ?: 0f) / 100f,
                activeColor = aqiColorProvider,
                backgroundColor = ColorProvider(
                    day = aqiColor.copy(alpha = 0.2f),
                    night = aqiColor.copy(alpha = 0.2f)
                ),
                modifier = GlanceModifier.padding(
                    start = 1.dp,
                    bottom = SIMPLE_WIDGET_AQI_PROGRESS_BOTTOM_PADDING
                ),
                totalWidth = progressBarWidth,
                config = config
            )
        }
    }
}

@Composable
private fun GlanceProgressBarWithDot(
    progress: Float,
    activeColor: GlanceColorProvider,
    backgroundColor: GlanceColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    totalWidth: androidx.compose.ui.unit.Dp = 100.dp,
    config: SimpleWidgetLayoutConfig
) {
    val geometry = calculateAqiProgressGeometry(
        requestedWidth = totalWidth,
        glowSize = config.glowSize,
        progress = progress
    )

    Box(
        modifier = modifier
            .width(geometry.totalWidth)
            .height(config.glowSize),
        contentAlignment = Alignment.CenterStart
    ) {
        // Progress track
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(config.barHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .width(geometry.activeWidth)
                    .fillMaxHeight()
                    .background(activeColor)
            ) {}
            Box(
                modifier = GlanceModifier
                    .width(geometry.inactiveWidth)
                    .fillMaxHeight()
                    .background(backgroundColor)
            ) {}
        }

        // Indicator Dot and Glow
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(config.glowSize),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = GlanceModifier.width(geometry.dotOffset))

            Box(
                modifier = GlanceModifier.size(config.glowSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_glow_circle),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxSize(),
                    colorFilter = ColorFilter.tint(activeColor)
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_white_circle),
                    contentDescription = null,
                    modifier = GlanceModifier.size(config.dotSize),
                    colorFilter = ColorFilter.tint(activeColor)
                )
            }
        }
    }
}

internal data class AqiProgressGeometry(
    val totalWidth: Dp,
    val activeWidth: Dp,
    val inactiveWidth: Dp,
    val dotOffset: Dp
)

internal fun calculateAqiProgressGeometry(
    requestedWidth: Dp,
    glowSize: Dp,
    progress: Float
): AqiProgressGeometry {
    val safeGlow = glowSize.value.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
    val requested = requestedWidth.value.takeIf { it.isFinite() } ?: safeGlow
    val total = requested.coerceIn(safeGlow, maxOf(100f, safeGlow))
    val normalizedProgress = progress.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
    val active = total * normalizedProgress
    val maxDotOffset = (total - safeGlow).coerceAtLeast(0f)
    val dotOffset = (active - (safeGlow / 2f)).coerceIn(0f, maxDotOffset)

    return AqiProgressGeometry(
        totalWidth = total.dp,
        activeWidth = active.dp,
        inactiveWidth = (total - active).dp,
        dotOffset = dotOffset.dp
    )
}

private fun positiveDp(value: Dp): Dp = value.value.coerceAtLeast(1f).dp

internal fun calculateTimestampMaxWidth(
    widgetWidth: Dp,
    contentStartPadding: Dp,
    refreshEndInset: Dp,
    inlineGap: Dp
): Dp = positiveDp(
    widgetWidth - contentStartPadding - refreshEndInset - inlineGap
)

private fun validDimensionOrFallback(value: Dp, fallback: Dp): Dp =
    value.value.takeIf { it.isFinite() && it > 0f }?.dp ?: fallback
