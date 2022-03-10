package com.ruuvi.station.firebase.domain

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.network.data.response.UserInfoResponseBody
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.util.BackgroundScanModes
import com.ruuvi.station.widgets.domain.WidgetsService
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception

class FirebaseInteractor(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val preferences: PreferencesRepository,
    private val tagInteractor: TagInteractor,
    private val sensorSettingsRepository: SensorSettingsRepository
) {
    fun saveUserProperties() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(15000)
            Timber.d("FirebasePropertySaver.saveUserProperties")
            try {
                firebaseAnalytics.setUserProperty(
                    BACKGROUND_SCAN_ENABLED,
                    (preferences.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND).toString()
                )
                firebaseAnalytics.setUserProperty(
                    BACKGROUND_SCAN_INTERVAL,
                    preferences.getBackgroundScanInterval().toString()
                )
                firebaseAnalytics.setUserProperty(
                    GATEWAY_ENABLED,
                    preferences.getDataForwardingUrl().isNotEmpty().toString()
                )
                firebaseAnalytics.setUserProperty(
                    TEMPERATURE_UNIT,
                    preferences.getTemperatureUnit().code
                )
                firebaseAnalytics.setUserProperty(
                    HUMIDITY_UNIT,
                    preferences.getHumidityUnit().code.toString()
                )
                firebaseAnalytics.setUserProperty(
                    PRESSURE_UNIT,
                    preferences.getPressureUnit().code.toString()
                )
                firebaseAnalytics.setUserProperty(
                    DASHBOARD_ENABLED,
                    preferences.isDashboardEnabled().toString()
                )
                firebaseAnalytics.setUserProperty(
                    GRAPH_POINT_INTERVAL,
                    preferences.getGraphPointInterval().toString()
                )
                firebaseAnalytics.setUserProperty(
                    GRAPH_VIEW_PERIOD,
                    preferences.getGraphViewPeriodDays().toString()
                )
                firebaseAnalytics.setUserProperty(
                    GRAPH_SHOW_ALL_POINTS,
                    preferences.isShowAllGraphPoint().toString()
                )
                firebaseAnalytics.setUserProperty(
                    GRAPH_DRAW_DOTS,
                    preferences.graphDrawDots().toString()
                )

                val favouriteTags = tagInteractor.getTagEntities(true)
                val addedTags = favouriteTags.size
                val notAddedTags = tagInteractor.getTagEntities(false).size
                val seenTags = addedTags + notAddedTags

                firebaseAnalytics.setUserProperty(SEEN_TAGS, seenTags.toString())

                firebaseAnalytics.setUserProperty(ADDED_TAGS, addedTags.toString())

                val userEmail = preferences.getUserEmail()
                val loggedIn = userEmail.isNotEmpty()
                firebaseAnalytics.setUserProperty(LOGGED_IN, loggedIn.toString())


                val sensorSettings = sensorSettingsRepository.getSensorSettings()
                val claimedSensors =
                    sensorSettings.count { it.networkSensor && it.owner == userEmail }
                val offlineSensors =
                    sensorSettings.count { !it.networkSensor }

                firebaseAnalytics.setUserProperty(CLAIMED_TAGS, claimedSensors.toString())
                firebaseAnalytics.setUserProperty(OFFLINE_TAGS, offlineSensors.toString())

                firebaseAnalytics.setUserProperty(USE_DF2, favouriteTags.any { it.dataFormat == 2 }.toString())
                firebaseAnalytics.setUserProperty(USE_DF3, favouriteTags.any { it.dataFormat == 3 }.toString())
                firebaseAnalytics.setUserProperty(USE_DF4, favouriteTags.any { it.dataFormat == 4 }.toString())
                firebaseAnalytics.setUserProperty(USE_DF5, favouriteTags.any { it.dataFormat == 5 }.toString())

                val useSimpleWidget = WidgetsService.getSimpleWidgetsIds(context).isNotEmpty()
                Timber.d("useSimpleWidget $useSimpleWidget")
                firebaseAnalytics.setUserProperty(USE_SIMPLE_WIDGET, useSimpleWidget.toString())
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    fun logSignIn() {
        CoroutineScope(Dispatchers.IO).launch {
            val addedTags = tagInteractor.getTagEntities(true).size
            val notAddedTags = tagInteractor.getTagEntities(false).size
            val seenTags = addedTags + notAddedTags

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
                param(SENSORS_ADDED, addedTags.toLong())
                param(SENSORS_SEEN, seenTags.toLong())
            }
        }
    }

    fun logSync(userInfoData: UserInfoResponseBody) {
        CoroutineScope(Dispatchers.IO).launch {
            val claimed = userInfoData.sensors.count { it.owner == userInfoData.email }
            val notOwned = userInfoData.sensors.count { it.owner != userInfoData.email }
            firebaseAnalytics.logEvent("sync") {
                param(SENSORS_CLAIMED, claimed.toLong())
                param(SENSORS_SHARED_TO_USER, notOwned.toLong())
            }
        }
    }

    fun logGattSync(size: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val emptyHistory = size == 0
            firebaseAnalytics.logEvent("gatt_sync") {
                param(GATT_SYNC_COUNT, size.toLong())
                param(GATT_EMPTY_HISTORY, emptyHistory.toString())
            }
        }
    }

    companion object {
        const val BACKGROUND_SCAN_ENABLED = "background_scan_enabled"
        const val BACKGROUND_SCAN_INTERVAL = "background_scan_interval"
        const val GATEWAY_ENABLED = "gateway_enabled"
        const val TEMPERATURE_UNIT = "temperature_unit"
        const val HUMIDITY_UNIT = "humidity_unit"
        const val PRESSURE_UNIT = "pressure_unit"
        const val DASHBOARD_ENABLED = "dashboard_enabled"
        const val GRAPH_POINT_INTERVAL = "graph_point_interval"
        const val GRAPH_VIEW_PERIOD = "graph_view_period"
        const val GRAPH_SHOW_ALL_POINTS = "graph_show_all_points"
        const val GRAPH_DRAW_DOTS = "graph_draw_dots"
        const val SEEN_TAGS = "seen_tags"
        const val ADDED_TAGS = "added_tags"
        const val LOGGED_IN = "logged_in"
        const val CLAIMED_TAGS = "claimed_tags"
        const val OFFLINE_TAGS = "offline_tags"
        const val USE_DF2 = "use_df2"
        const val USE_DF3 = "use_df3"
        const val USE_DF4 = "use_df4"
        const val USE_DF5 = "use_df5"
        const val USE_SIMPLE_WIDGET = "use_simple_widget"

        const val SENSORS_ADDED = "sensors_added"
        const val SENSORS_SEEN = "sensors_seen"
        const val SENSORS_CLAIMED = "sensors_claimed"
        const val SENSORS_SHARED_TO_USER = "sensors_shared_to_user"
        const val GATT_SYNC_COUNT = "gatt_sync_count"
        const val GATT_EMPTY_HISTORY = "gatt_empty_history"
    }
}