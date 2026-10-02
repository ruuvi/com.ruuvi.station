package com.ruuvi.station.network.domain

import com.ruuvi.station.network.data.response.RuuviNetworkResponse
import com.ruuvi.station.network.data.response.SensorDenseResponse
import com.ruuvi.station.network.data.response.SensorSubscription
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import com.ruuvi.station.network.data.response.SensorsDenseResponseBody

internal const val SYNC_SENSOR_ID = "AA:BB:CC:DD:EE:FF"
internal const val SYNC_USER_EMAIL = "owner@example.com"

internal fun denseSensor(sensorId: String = SYNC_SENSOR_ID) = SensorsDenseInfo(
    sensor = sensorId,
    owner = SYNC_USER_EMAIL,
    name = "Cloud sensor",
    picture = "",
    public = false,
    canShare = true,
    offsetTemperature = 0.0,
    offsetHumidity = 0.0,
    offsetPressure = 0.0,
    measurements = emptyList(),
    alerts = emptyList(),
    lastUpdated = 100L,
    subscription = SensorSubscription(
        maxHistoryDays = 0,
        maxResolutionMinutes = 1,
        emailAlertAllowed = false,
        pushAlertAllowed = false,
        subscriptionName = "free",
    ),
    settings = null,
    sharedTo = emptyList(),
    sharedToPending = emptyList(),
)

internal fun denseResponse(vararg sensors: SensorsDenseInfo): SensorDenseResponse =
    RuuviNetworkResponse(
        result = RuuviNetworkResponse.successResult,
        error = "",
        data = SensorsDenseResponseBody(sensors.toList()),
        code = null,
    )
