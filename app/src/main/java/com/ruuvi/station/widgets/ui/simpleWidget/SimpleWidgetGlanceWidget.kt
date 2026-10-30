package com.ruuvi.station.widgets.ui.simpleWidget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.core.util.TypedValueCompat
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
import androidx.glance.appwidget.state.updateAppWidgetState
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
import com.ruuvi.station.widgets.ui.glance.WidgetTextMetrics
import com.ruuvi.station.widgets.ui.glance.calculateAllocationSafeBitmapWidth
import com.ruuvi.station.widgets.ui.glance.fontBitmapFormat
import com.ruuvi.station.widgets.ui.glance.getZoomFactor
import com.ruuvi.station.widgets.ui.glance.scaledBy
import com.ruuvi.station.widgets.ui.glance.toWidgetSp
import com.ruuvi.station.widgets.update.WidgetRefreshScheduler
import com.ruuvi.station.widgets.update.resolveAppWidgetId
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import timber.log.Timber

object SimpleWidgetGlanceWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    override val sizeMode = SizeMode.Exact

    override val previewSizeMode: PreviewSizeMode =
        SizeMode.Responsive(setOf(DpSize(180.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        observeTypographySize(context, id)
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
                observedCellSize = observedSimpleWidgetTypographySize(
                    width = prefs[SimpleWidgetPrefKeys.typographyWidth],
                    height = prefs[SimpleWidgetPrefKeys.typographyHeight]
                ),
            )
        }
    }

    private suspend fun observeTypographySize(context: Context, glanceId: GlanceId) {
        val applicationContext = context.applicationContext
        val appWidgetId = GlanceAppWidgetManager(applicationContext).getAppWidgetId(glanceId)
        val options = AppWidgetManager.getInstance(applicationContext).getAppWidgetOptions(appWidgetId)
        val observedWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
        val observedHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        if (observedWidth <= 0 || observedHeight <= 0) return

        updateAppWidgetState(applicationContext, glanceId) { preferences ->
            if (preferences[SimpleWidgetPrefKeys.typographySizeObserved] != true) {
                preferences[SimpleWidgetPrefKeys.typographySizeObserved] = true
                preferences[SimpleWidgetPrefKeys.typographyWidth] = observedWidth
                preferences[SimpleWidgetPrefKeys.typographyHeight] = observedHeight
            }
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
internal fun SimpleWidgetContent(
    sensorId: String?,
    displayName: String,
    sensorValue: String,
    unit: String,
    measurementName: String,
    updated: String,
    measurementType: WidgetType?,
    renderingMode: SimpleWidgetRenderingMode,
    typographyScale: Float = 1f,
    observedCellSize: DpSize? = null,
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
        valueColor = { GlanceColors.resolvedSimpleWidgetSensorTitleColor(context) },
        unitColor = { GlanceColors.resolvedValueColor(context) },
    )
    val zoomFactor = getZoomFactor(context)
    val size = LocalSize.current
    val widgetWidth = validDimensionOrFallback(size.width, 110.dp)
    val widgetHeight = validDimensionOrFallback(size.height, 45.dp)
    val typographySize = simpleWidgetTypographySize(
        currentSize = DpSize(widgetWidth, widgetHeight),
        observedSize = observedCellSize
    )
    val baseConfig = SimpleWidgetLayoutConfig.fromSize(
        width = typographySize.width,
        height = typographySize.height,
        isTablet = context.resources.configuration.smallestScreenWidthDp >= 600,
        zoomFactor = zoomFactor
    )
    val scaledConfig = baseConfig.copy(
        displayNameFontSize = baseConfig.displayNameFontSize.scaledBy(typographyScale),
        valueFontSize = baseConfig.valueFontSize.scaledBy(typographyScale),
        secondaryFontSize = baseConfig.secondaryFontSize.scaledBy(typographyScale),
        timestampFontSize = baseConfig.timestampFontSize.scaledBy(typographyScale),
        measurementNameFontSize = baseConfig.measurementNameFontSize.scaledBy(typographyScale)
    )
    val isAirQuality = measurementType == WidgetType.AIR_QUALITY
    val config = fitSimpleWidgetConfig(
        context = context,
        config = scaledConfig,
        size = DpSize(widgetWidth, widgetHeight),
        sensorValue = sensorValue,
        unit = unit,
        measurementName = measurementName,
        isAirQuality = isAirQuality,
        embeddedValueColor = embeddedColors.value,
        observedCellSize = observedCellSize
    )
    val displayNameFontSize = config.displayNameFontSize.toWidgetSp(context)
    val timestampFontSize = config.timestampFontSize.toWidgetSp(context)
    val timestampHeight = GlanceFontUtils.measureSystemFontHeight(
        context = context,
        fontSize = timestampFontSize,
        bold = false
    )
    val readingLayout = if (isAirQuality) {
        measureAqiLayout(context, config, measurementName, sensorValue.substringBefore("/"))
    } else {
        measureMeasurementLayout(context, config, unit, measurementName, sensorValue)
    }
    val visibility = calculateSimpleWidgetContentVisibility(
        widgetHeight = widgetHeight,
        config = config,
        textHeights = SimpleWidgetTextHeights(
            displayName = GlanceFontUtils.measureSystemFontHeight(
                context = context,
                fontSize = displayNameFontSize,
                bold = true
            ),
            value = readingLayout.textLayout.height,
            secondary = timestampHeight
        ),
        hasUnit = unit.isNotBlank(),
        isAirQuality = measurementType == WidgetType.AIR_QUALITY,
        primaryContentMandatory = config.style == SimpleWidgetLayoutStyle.BALANCED_SHORT
    )

    val typographyAvailableWidth = simpleWidgetTypographyAvailableWidth(
        currentSize = DpSize(widgetWidth, widgetHeight),
        observedSize = observedCellSize,
        horizontalPadding = config.horizontalPadding
    )
    val readingAvailableWidth = simpleWidgetReadingAvailableWidth(
        currentSize = DpSize(widgetWidth, widgetHeight),
        observedSize = observedCellSize,
        horizontalPadding = config.horizontalPadding
    )
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
                        color = GlanceColors.simpleWidgetSensorTitle,
                        fontSize = displayNameFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(TITLE_VALUE_SPACING))
            }

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName,
                    config = config,
                    typographyAvailableWidth = typographyAvailableWidth,
                    readingAvailableWidth = readingAvailableWidth,
                    layout = readingLayout,
                    embeddedColors = embeddedColors,
                )
            } else {
                GlanceMeasurementDisplay(
                    sensorValue = sensorValue,
                    unit = unit,
                    measurementName = measurementName,
                    config = config,
                    typographyAvailableWidth = typographyAvailableWidth,
                    readingAvailableWidth = readingAvailableWidth,
                    showMeasurementDescription = visibility.showMeasurementDescription,
                    layout = readingLayout,
                    embeddedColors = embeddedColors,
                )
            }

            if (visibility.showTimestamp) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    TimestampText(updated, timestampMaxWidth, timestampFontSize)
                }
            }
        }

        RefreshButton(
            backingColor = GlanceColors.background,
            action = actionRunCallback<RefreshSimpleWidgetAction>()
        )
    }
}

@Composable
private fun TimestampText(updated: String, maxWidth: Dp, fontSize: TextUnit) {
    Text(
        text = updated,
        modifier = GlanceModifier.width(maxWidth),
        style = TextStyle(
            color = GlanceColors.widgetSensorName,
            fontSize = fontSize
        ),
        maxLines = 1
    )
}

internal enum class SimpleWidgetRenderingMode {
    RUNTIME,
    GENERATED_PREVIEW,
}

internal data class SimpleWidgetEmbeddedColors(
    val value: Int?,
    val unit: Int?,
)

internal fun resolveSimpleWidgetEmbeddedColors(
    renderingMode: SimpleWidgetRenderingMode,
    valueColor: () -> Int,
    unitColor: () -> Int,
): SimpleWidgetEmbeddedColors =
    when (renderingMode) {
        SimpleWidgetRenderingMode.RUNTIME -> SimpleWidgetEmbeddedColors(
            value = null,
            unit = null,
        )
        SimpleWidgetRenderingMode.GENERATED_PREVIEW -> SimpleWidgetEmbeddedColors(
            value = valueColor(),
            unit = unitColor(),
        )
    }

@Composable
private fun GlanceMeasurementDisplay(
    sensorValue: String,
    unit: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    typographyAvailableWidth: Dp,
    readingAvailableWidth: Dp,
    showMeasurementDescription: Boolean,
    layout: MeasuredSimpleWidgetLayout,
    embeddedColors: SimpleWidgetEmbeddedColors
) {
    val context = LocalContext.current
    val valueMaxWidth = simpleWidgetValueMaxWidth(
        config,
        typographyAvailableWidth,
        layout.minimumLabelWidth
    )
    val refreshOverlap = calculateRefreshOverlap(
        contentEndPadding = config.horizontalPadding,
        refreshTouchTargetSize = WidgetRefreshButtonDefaults.touchTargetSize
    )

    Row(
        modifier = GlanceModifier.width(readingAvailableWidth),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = sensorValue,
            fontSize = layout.valueFontSize,
            colorProvider = GlanceColors.simpleWidgetSensorTitleBitmapTint,
            fontResId = R.font.oswald_bold,
            maxWidth = valueMaxWidth,
            embeddedColor = embeddedColors.value,
            trimVerticalPadding = true,
            stableNumericBounds = true
        )

        if (unit.isNotBlank() || showMeasurementDescription) {
            Spacer(modifier = GlanceModifier.width(config.inlineSpacing * 2))

            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(layout.textLayout.height)
            ) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = layout.textLayout.unitTopPadding)
                ) {
                    if (unit.isNotBlank()) {
                        CustomFontText(
                            text = unit,
                            fontSize = layout.labelFontSize,
                            colorProvider = GlanceColors.valueColor,
                            fontResId = R.font.oswald_regular,
                            maxWidth = GlanceFontUtils.measureCustomFontWidth(
                                context = context,
                                text = unit,
                                fontSize = layout.labelFontSize,
                                fontResId = R.font.oswald_regular
                            ),
                            embeddedColor = embeddedColors.unit,
                            trimVerticalPadding = true
                        )
                    }
                }

                if (showMeasurementDescription && measurementName.isNotBlank()) {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(
                                top = layout.textLayout.measurementTopPadding,
                                end = refreshOverlap
                            )
                    ) {
                        Text(
                            text = measurementName,
                            modifier = GlanceModifier.fillMaxWidth(),
                            style = TextStyle(
                                color = GlanceColors.widgetSensorName,
                                fontSize = layout.measurementNameFontSize
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

internal fun measureMeasurementLayout(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    unit: String,
    measurementName: String,
    sensorValue: String = "0"
): MeasuredSimpleWidgetLayout =
    measureSimpleWidgetLayout(context, config, unit, measurementName, 0.dp, sensorValue)

private const val GENERATED_PREVIEW_TYPOGRAPHY_SCALE = 0.75f
private const val AQI_UNIT_TEXT = "/100"
private val AQI_INDICATOR_SIZE = 8.dp
private val TITLE_VALUE_SPACING = 7.dp
private val VALUE_SIZING_TITLE_SPACING = 5.dp

internal data class MeasuredSimpleWidgetLayout(
    val valueFontSize: TextUnit,
    val labelFontSize: TextUnit,
    val measurementNameFontSize: TextUnit,
    val labelHeights: SimpleWidgetLabelHeights,
    val textLayout: SimpleWidgetTextLayout,
    val minimumLabelWidth: Dp,
    val indicatorSize: Dp
)

private fun simpleWidgetValueMaxWidth(
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp,
    minimumLabelWidth: Dp
): Dp {
    val spacing = if (minimumLabelWidth > 0.dp) config.inlineSpacing * 2 else 0.dp
    return positiveDp(availableWidth - minimumLabelWidth - spacing)
}

internal fun fitSimpleWidgetConfig(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    size: DpSize,
    sensorValue: String,
    unit: String,
    measurementName: String,
    isAirQuality: Boolean,
    embeddedValueColor: Int? = null,
    observedCellSize: DpSize? = null
): SimpleWidgetLayoutConfig {
    val metrics = context.resources.displayMetrics
    val density = metrics.density
    val typographySize = simpleWidgetTypographySize(size, observedCellSize)
    val availableHeightPx = (typographySize.height.value * density).roundToInt() -
        2 * (config.verticalPadding.value * density).roundToInt() -
        (VALUE_SIZING_TITLE_SPACING.value * density).roundToInt()
    val availableWidthPx = (typographySize.width.value * density).roundToInt() -
        2 * (config.horizontalPadding.value * density).roundToInt()
    val valueText = if (isAirQuality) sensorValue.substringBefore("/") else sensorValue
    val unitText = if (isAirQuality) AQI_UNIT_TEXT else unit
    val preferredConfig = config.copy(valueFontSize = calculateAqiValueFontSize(config))
    val bitmapBytesPerPixel = fontBitmapFormat(embeddedValueColor).bytesPerPixel

    fun fitsBitmap(textMetrics: WidgetTextMetrics): Boolean {
        val maximumWidth = calculateAllocationSafeBitmapWidth(
            bitmapHeight = (textMetrics.height.value * density).roundToInt(),
            bytesPerPixel = bitmapBytesPerPixel
        )
        return ceil(textMetrics.width.value * density) <= maximumWidth
    }

    fun measureLayout(candidate: SimpleWidgetLayoutConfig): MeasuredSimpleWidgetLayout =
        if (isAirQuality) {
            measureAqiLayout(context, candidate, measurementName, valueText)
        } else {
            measureMeasurementLayout(context, candidate, unit, measurementName, valueText)
        }

    fun withReducedValue(candidate: SimpleWidgetLayoutConfig): SimpleWidgetLayoutConfig =
        candidate.copy(
            valueFontSize = reduceSimpleWidgetValueFontSize(
                candidate.valueFontSize.toWidgetSp(context)
            ).scaledBy(getZoomFactor(context))
        )

    val maximumValueFontSize = GlanceFontUtils.fitCustomFontSizeToBaselineHeight(
        context = context,
        fontSize = preferredConfig.valueFontSize.toWidgetSp(context),
        fontResId = R.font.oswald_bold,
        minimumHeight = (availableHeightPx / density).dp
    ).scaledBy(getZoomFactor(context))

    val fitted = fitSimpleWidgetTypography(preferredConfig, maximumValueFontSize) { candidate ->
        val displayNameHeight = GlanceFontUtils.measureSystemFontHeight(
            context, candidate.displayNameFontSize.toWidgetSp(context), bold = true
        )
        val timestampHeight = GlanceFontUtils.measureSystemFontHeight(
            context, candidate.timestampFontSize.toWidgetSp(context), bold = false
        )
        val layout = measureLayout(candidate)
        val valueMaxWidth = simpleWidgetValueMaxWidth(
            candidate, (availableWidthPx / density).dp, layout.minimumLabelWidth
        )
        val valueMetrics = GlanceFontUtils.measureCustomFontMetrics(
            context, layout.valueFontSize, R.font.oswald_bold, valueText,
            trimVerticalPadding = true, stableNumericBounds = true
        )
        val unitMetrics = GlanceFontUtils.measureCustomFontMetrics(
            context, layout.labelFontSize, R.font.oswald_regular, unitText, trimVerticalPadding = true
        )
        val labelSpacing = if (isAirQuality || unit.isNotBlank() || measurementName.isNotBlank()) {
            config.inlineSpacing * 2
        } else {
            0.dp
        }
        val availableReadingHeightPx = availableHeightPx -
            (displayNameHeight.value * density).roundToInt() -
            (timestampHeight.value * density).roundToInt()

        ceil(valueMetrics.height.value * density) <= availableReadingHeightPx &&
            valueMetrics.width <= valueMaxWidth &&
            fitsBitmap(valueMetrics) &&
            fitsBitmap(unitMetrics) &&
            ceil(valueMetrics.width.value * density) +
            ceil(layout.minimumLabelWidth.value * density) +
            (labelSpacing.value * density).roundToInt() <= availableWidthPx &&
            (
                ceil(layout.textLayout.height.value * density) <= availableReadingHeightPx ||
                    ceil(measureLayout(withReducedValue(candidate)).textLayout.height.value * density) <=
                    availableReadingHeightPx
            )
    }
    return withReducedValue(fitted)
}

internal fun observedSimpleWidgetTypographySize(width: Int?, height: Int?): DpSize? =
    if (width != null && width > 0 && height != null && height > 0) {
        DpSize(width.dp, height.dp)
    } else {
        null
    }

internal fun simpleWidgetTypographySize(currentSize: DpSize, observedSize: DpSize?): DpSize =
    observedSize ?: currentSize

internal fun simpleWidgetTypographyAvailableWidth(
    currentSize: DpSize,
    observedSize: DpSize?,
    horizontalPadding: Dp
): Dp = positiveDp(
    simpleWidgetTypographySize(currentSize, observedSize).width - (horizontalPadding * 2)
)

internal fun simpleWidgetReadingAvailableWidth(
    currentSize: DpSize,
    observedSize: DpSize?,
    horizontalPadding: Dp
): Dp = maxOf(
    positiveDp(currentSize.width - (horizontalPadding * 2)),
    simpleWidgetTypographyAvailableWidth(currentSize, observedSize, horizontalPadding)
)

internal fun measureAqiLayout(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    measurementName: String,
    sensorValue: String = "0"
): MeasuredSimpleWidgetLayout =
    measureSimpleWidgetLayout(context, config, AQI_UNIT_TEXT, measurementName, AQI_INDICATOR_SIZE, sensorValue)

private fun measureSimpleWidgetLayout(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    unit: String,
    measurementName: String,
    indicatorSize: Dp,
    sensorValue: String
): MeasuredSimpleWidgetLayout {
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density
    var valueFontSize = config.valueFontSize.toWidgetSp(context)
    var valueMetrics = GlanceFontUtils.measureCustomFontMetrics(
        context, valueFontSize, R.font.oswald_bold, sensorValue,
        trimVerticalPadding = true, stableNumericBounds = true
    )
    // Both label fonts need at least two physical pixels, even in the smallest widgets.
    if ((valueMetrics.height.value * density).roundToInt() < 4) {
        valueFontSize = GlanceFontUtils.fitCustomFontSizeToBaselineHeight(
            context, valueFontSize, R.font.oswald_bold, minimumHeight = (2f / density).dp
        )
        valueMetrics = GlanceFontUtils.measureCustomFontMetrics(
            context, valueFontSize, R.font.oswald_bold, sensorValue,
            trimVerticalPadding = true, stableNumericBounds = true
        )
    }
    val labelHeights = calculateSimpleWidgetLabelHeights(valueMetrics.height, density)

    fun measureMeasurementName(fontSize: TextUnit): WidgetTextMetrics =
        if (measurementName.isBlank()) {
            WidgetTextMetrics(0.dp, 0.dp, 0.dp, 0.dp)
        } else {
            GlanceFontUtils.measureSystemFontMetrics(
                context, measurementName, fontSize, bold = false
            )
        }

    fun measureUnit(fontSize: TextUnit): WidgetTextMetrics =
        if (unit.isBlank()) {
            WidgetTextMetrics(0.dp, 0.dp, 0.dp, 0.dp)
        } else {
            GlanceFontUtils.measureCustomFontMetrics(
                context, fontSize, R.font.oswald_regular, unit, trimVerticalPadding = true
            )
        }

    fun fitLabel(
        maximumHeightPx: Int,
        measure: (TextUnit) -> WidgetTextMetrics
    ): TextUnit = fitSimpleWidgetLabelFontSize(
        maximumFontSize = TypedValueCompat.pxToSp(maximumHeightPx * 2f, displayMetrics).sp
    ) { fontSize ->
        (measure(fontSize).height.value * density).roundToInt() <= maximumHeightPx
    }

    val labelFontSize = if (unit.isBlank()) {
        config.secondaryFontSize.toWidgetSp(context)
    } else {
        fitLabel(
            (labelHeights.unit.value * density).roundToInt(),
            ::measureUnit
        )
    }
    val preferredMeasurementFontSize = config.measurementNameFontSize.toWidgetSp(context)
    val measurementNameFontSize = if (measurementName.isBlank()) {
        preferredMeasurementFontSize
    } else {
        fitLabel(
            (labelHeights.measurement.value * density).roundToInt(),
            ::measureMeasurementName
        )
    }
    val unitMetrics = measureUnit(labelFontSize)
    val measurementMetrics = measureMeasurementName(measurementNameFontSize)
    val fittedIndicatorSize = minOf(indicatorSize, unitMetrics.height)
    val indicatorWidth = if (indicatorSize > 0.dp) {
        config.inlineSpacing * 2 + fittedIndicatorSize
    } else {
        0.dp
    }
    // Let long descriptions ellipsize rather than shrinking the value when their text grows vertically.
    val minimumMeasurementWidth = minOf(
        measurementMetrics.width,
        measureMeasurementName(preferredMeasurementFontSize).width
    )
    return MeasuredSimpleWidgetLayout(
        valueFontSize = valueFontSize,
        labelFontSize = labelFontSize,
        measurementNameFontSize = measurementNameFontSize,
        labelHeights = labelHeights,
        textLayout = calculateSimpleWidgetTextLayout(valueMetrics, unitMetrics, measurementMetrics, density),
        minimumLabelWidth = maxOf(unitMetrics.width + indicatorWidth, minimumMeasurementWidth),
        indicatorSize = fittedIndicatorSize
    )
}

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    typographyAvailableWidth: Dp,
    readingAvailableWidth: Dp,
    layout: MeasuredSimpleWidgetLayout,
    embeddedColors: SimpleWidgetEmbeddedColors
) {
    val context = LocalContext.current
    val aqiText = sensorValue.substringBefore("/")
    val aqiValue = aqiText.toDoubleOrNull()?.takeIf { it.isFinite() }
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = ColorProvider(day = aqiColor, night = aqiColor)
    val valueMaxWidth = simpleWidgetValueMaxWidth(
        config,
        typographyAvailableWidth,
        layout.minimumLabelWidth
    )
    val refreshOverlap = calculateRefreshOverlap(
        contentEndPadding = config.horizontalPadding,
        refreshTouchTargetSize = WidgetRefreshButtonDefaults.touchTargetSize
    )

    Row(
        modifier = GlanceModifier.width(readingAvailableWidth),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = aqiText,
            fontSize = layout.valueFontSize,
            colorProvider = GlanceColors.simpleWidgetSensorTitleBitmapTint,
            fontResId = R.font.oswald_bold,
            maxWidth = valueMaxWidth,
            embeddedColor = embeddedColors.value,
            trimVerticalPadding = true,
            stableNumericBounds = true
        )

        Spacer(modifier = GlanceModifier.width(config.inlineSpacing * 2))

        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(layout.textLayout.height)
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = layout.textLayout.unitTopPadding)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CustomFontText(
                        text = AQI_UNIT_TEXT,
                        fontSize = layout.labelFontSize,
                        colorProvider = GlanceColors.valueColor,
                        fontResId = R.font.oswald_regular,
                        maxWidth = GlanceFontUtils.measureCustomFontWidth(
                            context = context,
                            text = AQI_UNIT_TEXT,
                            fontSize = layout.labelFontSize,
                            fontResId = R.font.oswald_regular
                        ),
                        embeddedColor = embeddedColors.unit,
                        trimVerticalPadding = true
                    )
                    Spacer(modifier = GlanceModifier.width(config.inlineSpacing * 2))
                    Image(
                        provider = ImageProvider(R.drawable.ic_white_circle),
                        contentDescription = null,
                        modifier = GlanceModifier.size(layout.indicatorSize),
                        colorFilter = ColorFilter.tint(aqiColorProvider)
                    )
                }
            }

            if (measurementName.isNotBlank()) {
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(
                            top = layout.textLayout.measurementTopPadding,
                            end = refreshOverlap
                        )
                ) {
                    Text(
                        text = measurementName,
                        modifier = GlanceModifier.fillMaxWidth(),
                        style = TextStyle(
                            color = GlanceColors.widgetSensorName,
                            fontSize = layout.measurementNameFontSize
                        ),
                        maxLines = 1
                    )
                }
            }
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

internal fun calculateRefreshOverlap(
    contentEndPadding: Dp,
    refreshTouchTargetSize: Dp
): Dp = (refreshTouchTargetSize - contentEndPadding).value.coerceAtLeast(0f).dp

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
