package com.ruuvi.station.widgets.data

/**
 * A sensor reading normalized to raw, calibrated units.
 *
 * This model deliberately contains no localized or rounded display strings. Derived measurements
 * such as AQI, absolute humidity, and dew point are calculated when a widget measurement is
 * formatted.
 */
internal data class WidgetSensorSnapshot(
    val sensorId: String,
    val displayName: String,
    val timestampEpochMillis: Long,
    val temperatureCelsius: Double? = null,
    val relativeHumidityPercent: Double? = null,
    val pressurePascal: Double? = null,
    val movementCount: Int? = null,
    val voltageVolt: Double? = null,
    val rssiDbm: Int? = null,
    val measurementSequenceNumber: Int? = null,
    val accelerationXG: Double? = null,
    val accelerationYG: Double? = null,
    val accelerationZG: Double? = null,
    val soundAverageDba: Double? = null,
    val soundPeakDba: Double? = null,
    val luminosityLux: Double? = null,
    val co2Ppm: Int? = null,
    val vocIndex: Int? = null,
    val noxIndex: Int? = null,
    val pm1MicrogramsPerCubicMeter: Double? = null,
    val pm2_5MicrogramsPerCubicMeter: Double? = null,
    val pm4MicrogramsPerCubicMeter: Double? = null,
    val pm10MicrogramsPerCubicMeter: Double? = null,
)
