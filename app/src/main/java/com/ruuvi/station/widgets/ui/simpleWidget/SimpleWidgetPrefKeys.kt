package com.ruuvi.station.widgets.ui.simpleWidget

import androidx.datastore.preferences.core.stringPreferencesKey

object SimpleWidgetPrefKeys {
    val sensorId = stringPreferencesKey("sensor_id")
    val displayName = stringPreferencesKey("display_name")
    val sensorValue = stringPreferencesKey("sensor_value")
    val unit = stringPreferencesKey("unit")
    val measurementName = stringPreferencesKey("measurement_name")
    val updated = stringPreferencesKey("updated")
    val measurementType = stringPreferencesKey("measurement_type")
}
