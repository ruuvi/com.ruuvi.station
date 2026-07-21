package com.ruuvi.station.tagdetails.ui

import androidx.annotation.StringRes
import com.ruuvi.station.R
import com.ruuvi.station.tagsettings.ui.SensorSettingsRoutes

/**
 * Public launch instructions kept for intent and pending-intent compatibility.
 *
 * [DEFAULT] and [REMOVE] are launch-only values. UI code resolves them into a
 * concrete [SensorDetailDestination] before rendering.
 */
enum class SensorCardOpenType {
    DEFAULT,
    CARD,
    HISTORY,
    ALERTS,
    SETTINGS,
    REMOVE,
}

internal enum class SensorDetailDestination(
    val allowsSensorSwipe: Boolean = false,
    val usesSensorBackground: Boolean = false,
    val showsSensorFooter: Boolean = false,
    @StringRes val subtitleRes: Int? = null,
) {
    CARD(
        allowsSensorSwipe = true,
        usesSensorBackground = true,
        showsSensorFooter = true,
    ),
    HISTORY(
        usesSensorBackground = true,
        showsSensorFooter = true,
    ),
    ALERTS(subtitleRes = R.string.alerts),
    SETTINGS(subtitleRes = R.string.settings),
}

internal data class SensorDetailStartDestination(
    val root: SensorDetailDestination,
    val settingsRoute: String = SensorSettingsRoutes.SENSOR_SETTINGS_ROOT,
)

internal fun SensorCardOpenType.resolveStartDestination(
    defaultShowsHistory: Boolean,
): SensorDetailStartDestination = when (this) {
    SensorCardOpenType.DEFAULT -> SensorDetailStartDestination(
        root = if (defaultShowsHistory) {
            SensorDetailDestination.HISTORY
        } else {
            SensorDetailDestination.CARD
        },
    )
    SensorCardOpenType.CARD -> SensorDetailStartDestination(SensorDetailDestination.CARD)
    SensorCardOpenType.HISTORY -> SensorDetailStartDestination(SensorDetailDestination.HISTORY)
    SensorCardOpenType.ALERTS -> SensorDetailStartDestination(SensorDetailDestination.ALERTS)
    SensorCardOpenType.SETTINGS -> SensorDetailStartDestination(SensorDetailDestination.SETTINGS)
    SensorCardOpenType.REMOVE -> SensorDetailStartDestination(
        root = SensorDetailDestination.SETTINGS,
        settingsRoute = SensorSettingsRoutes.SENSOR_REMOVE,
    )
}
