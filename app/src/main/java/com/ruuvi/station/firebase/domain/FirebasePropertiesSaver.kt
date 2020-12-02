package com.ruuvi.station.firebase.domain

import com.google.firebase.analytics.FirebaseAnalytics
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.tag.domain.TagInteractor
import com.ruuvi.station.util.BackgroundScanModes
import timber.log.Timber

class FirebasePropertiesSaver(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val preferences: PreferencesRepository,
    private val tagInteractor: TagInteractor

) {
    fun saveUserProperties() {
        Timber.d("FirebasePropertySaver.saveUserProperties")
        firebaseAnalytics.setUserProperty("background_scan_enabled",
            (preferences.getBackgroundScanMode() == BackgroundScanModes.BACKGROUND).toString())
        firebaseAnalytics.setUserProperty("background_scan_interval", preferences.getBackgroundScanInterval().toString())
        firebaseAnalytics.setUserProperty("gateway_enabled", preferences.getGatewayUrl().isNotEmpty().toString())
        firebaseAnalytics.setUserProperty("temperature_unit", preferences.getTemperatureUnit().name)
        firebaseAnalytics.setUserProperty("humidity_unit", preferences.getHumidityUnit().name)
        firebaseAnalytics.setUserProperty("pressure_unit", preferences.getPressureUnit().name)
        firebaseAnalytics.setUserProperty("dashboard_enabled", preferences.isDashboardEnabled().toString())
        firebaseAnalytics.setUserProperty("graph_point_interval", preferences.getGraphPointInterval().toString())
        firebaseAnalytics.setUserProperty("graph_view_period", preferences.getGraphViewPeriod().toString())
        firebaseAnalytics.setUserProperty("graph_show_all_points", preferences.isShowAllGraphPoint().toString())
        firebaseAnalytics.setUserProperty("graph_draw_dots", preferences.graphDrawDots().toString())

        val favouriteTagsCount = tagInteractor.getTagEntities(true).size
        val nonFavouriteTagsCount = tagInteractor.getTagEntities(false).size

        firebaseAnalytics.setUserProperty("seen_tags", (favouriteTagsCount + nonFavouriteTagsCount).toString())
        firebaseAnalytics.setUserProperty("added_tags", favouriteTagsCount.toString())
    }
}