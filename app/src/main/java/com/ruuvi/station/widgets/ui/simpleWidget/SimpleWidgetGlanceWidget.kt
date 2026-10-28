package com.ruuvi.station.widgets.ui.simpleWidget

import android.content.Context
import android.util.TypedValue
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
    val baseConfig = SimpleWidgetLayoutConfig.fromSize(
        width = widgetWidth,
        height = widgetHeight,
        isTablet = context.resources.configuration.smallestScreenWidthDp >= 600,
        zoomFactor = zoomFactor
    )
    val scaledConfig = baseConfig.copy(
        displayNameFontSize = baseConfig.displayNameFontSize.scaledBy(typographyScale),
        valueFontSize = baseConfig.valueFontSize.scaledBy(typographyScale),
        secondaryFontSize = baseConfig.secondaryFontSize.scaledBy(typographyScale),
        timestampFontSize = baseConfig.timestampFontSize.scaledBy(typographyScale)
    )
    val isAirQuality = measurementType == WidgetType.AIR_QUALITY
    val config = fitSimpleWidgetConfig(
        context = context,
        config = scaledConfig,
        size = DpSize(widgetWidth, widgetHeight),
        sensorValue = sensorValue,
        unit = unit,
        measurementName = measurementName,
        isAirQuality = isAirQuality
    )
    val displayNameFontSize = config.displayNameFontSize.toWidgetSp(context)
    val timestampFontSize = config.timestampFontSize.toWidgetSp(context)
    val timestampHeight = GlanceFontUtils.measureSystemFontHeight(
        context = context,
        fontSize = timestampFontSize,
        bold = false
    )
    val readingLayout = if (isAirQuality) {
        measureAqiLayout(context, config, measurementName)
    } else {
        measureMeasurementLayout(context, config, unit, measurementName)
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
                        color = GlanceColors.widgetSensorTitle,
                        fontSize = displayNameFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }

            if (config.style == SimpleWidgetLayoutStyle.BALANCED_SHORT) {
                Spacer(modifier = GlanceModifier.defaultWeight())
            }

            if (measurementType == WidgetType.AIR_QUALITY) {
                GlanceAQIDisplay(
                    sensorValue = sensorValue,
                    measurementName = measurementName,
                    config = config,
                    availableWidth = availableWidth,
                    layout = readingLayout,
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
                    layout = readingLayout,
                    embeddedValueColor = embeddedColors.value,
                )
            }

            if (visibility.showTimestamp) {
                if (config.style == SimpleWidgetLayoutStyle.BALANCED_SHORT) {
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    TimestampText(updated, timestampMaxWidth, timestampFontSize)
                } else {
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
    layout: MeasuredSimpleWidgetLayout,
    embeddedValueColor: Int?
) {
    val context = LocalContext.current
    val valueMaxWidth = simpleWidgetValueMaxWidth(config, availableWidth, unit, isAirQuality = false)
    val refreshOverlap = calculateRefreshOverlap(
        contentEndPadding = config.horizontalPadding,
        refreshTouchTargetSize = WidgetRefreshButtonDefaults.touchTargetSize
    )

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = sensorValue,
            fontSize = layout.valueFontSize,
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold,
            modifier = GlanceModifier.padding(top = layout.textLayout.valueTopPadding),
            maxWidth = valueMaxWidth,
            embeddedColor = embeddedValueColor
        )

        if (unit.isNotBlank() || showMeasurementDescription) {
            Spacer(modifier = GlanceModifier.width(config.inlineSpacing * 2))

            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(layout.textLayout.height)
                    .padding(end = refreshOverlap)
            ) {
                if (unit.isNotBlank()) {
                    CustomFontText(
                        text = unit,
                        fontSize = layout.labelFontSize,
                        colorProvider = GlanceColors.valueColor,
                        fontResId = R.font.oswald_regular,
                        modifier = GlanceModifier.padding(top = layout.textLayout.unitTopPadding),
                        maxWidth = GlanceFontUtils.measureCustomFontWidth(
                            context = context,
                            text = unit,
                            fontSize = layout.labelFontSize,
                            fontResId = R.font.oswald_regular
                        ),
                        embeddedColor = embeddedValueColor
                    )
                }

                if (showMeasurementDescription) {
                    Text(
                        text = measurementName,
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .padding(top = layout.textLayout.measurementTopPadding),
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

internal fun measureMeasurementLayout(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    unit: String,
    measurementName: String
): MeasuredSimpleWidgetLayout =
    measureSimpleWidgetLayout(context, config, unit, measurementName, indicatorSize = 0.dp)

private const val GENERATED_PREVIEW_TYPOGRAPHY_SCALE = 0.75f
private const val AQI_UNIT_TEXT = "/100"
private val AQI_INDICATOR_SIZE = 8.dp

internal data class MeasuredSimpleWidgetLayout(
    val valueFontSize: TextUnit,
    val labelFontSize: TextUnit,
    val measurementNameFontSize: TextUnit,
    val textLayout: SimpleWidgetAqiTextLayout,
    val labelWidth: Dp,
    val indicatorSize: Dp
)

private fun simpleWidgetValueMaxWidth(
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp,
    unit: String,
    isAirQuality: Boolean
): Dp = positiveDp(
    if (isAirQuality) {
        availableWidth * 0.5f
    } else {
        (availableWidth - config.inlineSpacing) * if (unit.isBlank()) 1f else 0.65f
    }
)

internal fun fitSimpleWidgetConfig(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    size: DpSize,
    sensorValue: String,
    unit: String,
    measurementName: String,
    isAirQuality: Boolean
): SimpleWidgetLayoutConfig {
    val density = context.resources.displayMetrics.density
    val availableHeightPx = (size.height.value * density).roundToInt() -
        2 * (config.verticalPadding.value * density).roundToInt()
    val availableWidthPx = (size.width.value * density).roundToInt() -
        2 * (config.horizontalPadding.value * density).roundToInt()
    val valueMaxWidth = simpleWidgetValueMaxWidth(
        config, (availableWidthPx / density).dp, unit, isAirQuality
    )
    val valueText = if (isAirQuality) sensorValue.substringBefore("/") else sensorValue
    val preferredConfig = config.copy(valueFontSize = calculateAqiValueFontSize(config))

    return fitSimpleWidgetTypography(preferredConfig) { candidate ->
        val displayNameHeight = GlanceFontUtils.measureSystemFontHeight(
            context, candidate.displayNameFontSize.toWidgetSp(context), bold = true
        )
        val timestampHeight = GlanceFontUtils.measureSystemFontHeight(
            context, candidate.timestampFontSize.toWidgetSp(context), bold = false
        )
        val layout = if (isAirQuality) {
            measureAqiLayout(context, candidate, measurementName)
        } else {
            measureMeasurementLayout(context, candidate, unit, measurementName)
        }
        val valueWidth = GlanceFontUtils.measureCustomFontWidth(
            context, valueText, layout.valueFontSize, R.font.oswald_bold
        )
        val labelSpacing = if (isAirQuality || unit.isNotBlank() || measurementName.isNotBlank()) {
            config.inlineSpacing * 2
        } else {
            0.dp
        }
        ceil(displayNameHeight.value * density) +
            ceil(layout.textLayout.height.value * density) +
            ceil(timestampHeight.value * density) <= availableHeightPx &&
            valueWidth <= valueMaxWidth &&
            ceil(valueWidth.value * density) +
            ceil(layout.labelWidth.value * density) +
            (labelSpacing.value * density).roundToInt() <= availableWidthPx
    }
}

internal fun measureAqiLayout(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    measurementName: String
): MeasuredSimpleWidgetLayout =
    measureSimpleWidgetLayout(context, config, AQI_UNIT_TEXT, measurementName, AQI_INDICATOR_SIZE)

private fun measureSimpleWidgetLayout(
    context: Context,
    config: SimpleWidgetLayoutConfig,
    unit: String,
    measurementName: String,
    indicatorSize: Dp
): MeasuredSimpleWidgetLayout {
    var labelFontSize = config.secondaryFontSize.toWidgetSp(context)
    var measurementNameFontSize = config.measurementNameFontSize.toWidgetSp(context)
    var valueFontSize = config.valueFontSize.toWidgetSp(context)

    fun measureMeasurementName(text: String): WidgetTextMetrics =
        if (text.isBlank()) {
            WidgetTextMetrics(0.dp, 0.dp, 0.dp, 0.dp)
        } else {
            GlanceFontUtils.measureSystemFontMetrics(
                context, text, measurementNameFontSize, bold = false
            )
        }

    fun measureUnit(): WidgetTextMetrics =
        if (unit.isBlank()) {
            WidgetTextMetrics(0.dp, 0.dp, 0.dp, 0.dp)
        } else {
            GlanceFontUtils.measureCustomFontMetrics(
                context, labelFontSize, R.font.oswald_regular
            ).copy(
                width = GlanceFontUtils.measureCustomFontWidth(
                    context, unit, labelFontSize, R.font.oswald_regular
                )
            )
        }

    var unitMetrics = measureUnit()
        var measurementMetrics = measureMeasurementName(measurementName)
    var fittedIndicatorSize = minOf(indicatorSize, unitMetrics.glyphBottom - unitMetrics.glyphTop)

    if (unit.isNotBlank() && measurementName.isNotBlank()) {
        val preferredValue = GlanceFontUtils.measureCustomFontMetrics(
            context, valueFontSize, R.font.oswald_bold
        )
        val labelHeight = calculateAqiMinimumValueHeight(
            unitMetrics, measurementMetrics, fittedIndicatorSize, spacing = 0.dp
        )
        if (labelHeight > 0.dp) {
            val metrics = context.resources.displayMetrics
            val targetHeight = (
                preferredValue.baseline - preferredValue.glyphTop - config.inlineSpacing
            ).coerceAtLeast((1f / metrics.density).dp)
            val labelSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, labelFontSize.value, metrics
            )
            labelFontSize = TypedValueCompat.pxToSp(
                labelSizePx * targetHeight.value / labelHeight.value, metrics
            ).sp
            measurementNameFontSize = (labelFontSize.value - 1f).coerceAtLeast(1f).sp
            unitMetrics = measureUnit()
            measurementMetrics = measureMeasurementName(measurementName)
            fittedIndicatorSize = minOf(
                indicatorSize, unitMetrics.glyphBottom - unitMetrics.glyphTop
            )
        }

        // Size the value to the label stack, rather than spreading fixed-size labels apart.
        valueFontSize = GlanceFontUtils.fitCustomFontSizeToBaselineHeight(
            context = context,
            fontSize = valueFontSize,
            fontResId = R.font.oswald_bold,
            minimumHeight = calculateAqiMinimumValueHeight(
                unitMetrics, measurementMetrics, fittedIndicatorSize, config.inlineSpacing
            )
        )
    }

    val indicatorWidth = if (indicatorSize > 0.dp) {
        config.inlineSpacing * 2 + fittedIndicatorSize
    } else {
        0.dp
    }
    return MeasuredSimpleWidgetLayout(
        valueFontSize = valueFontSize,
        labelFontSize = labelFontSize,
        measurementNameFontSize = measurementNameFontSize,
        textLayout = calculateAqiTextLayout(
            value = GlanceFontUtils.measureCustomFontMetrics(context, valueFontSize, R.font.oswald_bold),
            unit = unitMetrics,
            measurement = measurementMetrics,
            indicatorSize = fittedIndicatorSize,
            measurementUnitTopSpacing = measurementUnitTopSpacing(unit)
        ),
        labelWidth = maxOf(unitMetrics.width + indicatorWidth, measurementMetrics.width),
        indicatorSize = fittedIndicatorSize
    )
}

internal fun measurementUnitTopSpacing(unit: String): Dp =
    if (unit in UNITS_REQUIRING_MEASUREMENT_SPACING) {
        MEASUREMENT_UNIT_TOP_SPACING
    } else {
        0.dp
    }

private val UNITS_REQUIRING_MEASUREMENT_SPACING =
    setOf("g", "g/m³", "ppm", "µg/m³")
private val MEASUREMENT_UNIT_TOP_SPACING = 2.dp

@Composable
private fun GlanceAQIDisplay(
    sensorValue: String,
    measurementName: String,
    config: SimpleWidgetLayoutConfig,
    availableWidth: Dp,
    layout: MeasuredSimpleWidgetLayout,
    embeddedValueColor: Int?
) {
    val context = LocalContext.current
    val aqiText = sensorValue.substringBefore("/")
    val aqiValue = aqiText.toDoubleOrNull()?.takeIf { it.isFinite() }
    val aqiColor = aqiValue?.let { AQI.CalculatedAQI(it).color } ?: Color.Gray
    val aqiColorProvider = ColorProvider(day = aqiColor, night = aqiColor)
    val valueMaxWidth = simpleWidgetValueMaxWidth(config, availableWidth, AQI_UNIT_TEXT, isAirQuality = true)
    val refreshOverlap = calculateRefreshOverlap(
        contentEndPadding = config.horizontalPadding,
        refreshTouchTargetSize = WidgetRefreshButtonDefaults.touchTargetSize
    )

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CustomFontText(
            text = aqiText,
            fontSize = layout.valueFontSize,
            colorProvider = GlanceColors.valueColor,
            fontResId = R.font.oswald_bold,
            modifier = GlanceModifier.padding(top = layout.textLayout.valueTopPadding),
            maxWidth = valueMaxWidth,
            embeddedColor = embeddedValueColor
        )

        Spacer(modifier = GlanceModifier.width(config.inlineSpacing * 2))

        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .height(layout.textLayout.height)
                .padding(end = refreshOverlap)
        ) {
            Row(
                modifier = GlanceModifier.padding(top = layout.textLayout.unitTopPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    embeddedColor = embeddedValueColor
                )
                Spacer(modifier = GlanceModifier.width(config.inlineSpacing * 2))
                Image(
                    provider = ImageProvider(R.drawable.ic_white_circle),
                    contentDescription = null,
                    modifier = GlanceModifier.size(layout.indicatorSize),
                    colorFilter = ColorFilter.tint(aqiColorProvider)
                )
            }

            Text(
                text = measurementName,
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = layout.textLayout.measurementTopPadding),
                style = TextStyle(
                    color = GlanceColors.widgetSensorName,
                    fontSize = layout.measurementNameFontSize
                ),
                maxLines = 1
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
