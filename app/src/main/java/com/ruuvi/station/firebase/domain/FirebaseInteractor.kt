package com.ruuvi.station.firebase.domain

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.network.data.response.UserInfoResponseBody
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.util.BackgroundScanModes
import kotlinx.coroutines.*
import timber.log.Timber
import java.lang.Exception

class FirebaseInteractor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val preferences: PreferencesRepository,
    private val tagInteractor: TagInteractor

) {
    fun saveUserProperties() {
        CoroutineScope(Dispatchers.IO).launch {
            delay(15000)
            Timber.d("FirebasePropertySaver.saveUserProperties")
            try {
                firebaseAnalytics.setUserProperty("background_scan_enabled",
                    (preferences.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND).toString())
                firebaseAnalytics.setUserProperty("background_scan_interval", preferences.getBackgroundScanInterval().toString())
                firebaseAnalytics.setUserProperty("gateway_enabled", preferences.getGatewayUrl().isNotEmpty().toString())
                firebaseAnalytics.setUserProperty("temperature_unit", preferences.getTemperatureUnit().code)
                firebaseAnalytics.setUserProperty("humidity_unit", preferences.getHumidityUnit().code.toString())
                firebaseAnalytics.setUserProperty("pressure_unit", preferences.getPressureUnit().code.toString())
                firebaseAnalytics.setUserProperty("dashboard_enabled", preferences.isDashboardEnabled().toString())
                firebaseAnalytics.setUserProperty("graph_point_interval", preferences.getGraphPointInterval().toString())
                firebaseAnalytics.setUserProperty("graph_view_period", preferences.getGraphViewPeriodDays().toString())
                firebaseAnalytics.setUserProperty("graph_show_all_points", preferences.isShowAllGraphPoint().toString())
                firebaseAnalytics.setUserProperty("graph_draw_dots", preferences.graphDrawDots().toString())

                val addedTags = tagInteractor.getTagEntities(true).size
                val notAddedTags = tagInteractor.getTagEntities(false).size
                val seenTags = addedTags + notAddedTags

                if (seenTags < 10) {
                    firebaseAnalytics.setUserProperty("seen_tags", (addedTags + notAddedTags).toString())
                } else {
                    firebaseAnalytics.setUserProperty("seen_tags", "10+")
                }

                if (addedTags < 10) {
                    firebaseAnalytics.setUserProperty("added_tags", addedTags.toString())
                } else {
                    firebaseAnalytics.setUserProperty("added_tags", "10+")
                }

                val signedIn = preferences.getUserEmail().isEmpty()
                firebaseAnalytics.setUserProperty("signed_in", signedIn.toString())
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
                param("sensors_added", addedTags.toLong())
                param("sensors_seen", seenTags.toLong())
            }
        }
    }

    fun logSync(userInfoData: UserInfoResponseBody) {
        CoroutineScope(Dispatchers.IO).launch {
            val claimed = userInfoData.sensors.count { it.owner == userInfoData.email }
            val notOwned = userInfoData.sensors.count { it.owner != userInfoData.email }
            firebaseAnalytics.logEvent("sync") {
                param("sensors_claimed", claimed.toLong())
                param("sensors_shared_to_user", notOwned.toLong())
            }
        }
    }
}