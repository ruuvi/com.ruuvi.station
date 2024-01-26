package com.ruuvi.station.dashboard.domain

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.tag.domain.RuuviTag
import timber.log.Timber

class SensorsSortingInteractor (
    val preferences: PreferencesRepository
){
    fun sortSensors(sensors: List<RuuviTag>): List<RuuviTag> {
        val sortingOrder = getListOfSortedSensors()

        for (sens in sortingOrder) {
            Timber.d("dragGestureHandler - sortedResult $sens")
        }
        Timber.d("dragGestureHandler - sortedResult =========================")


        if (sortingOrder.isEmpty()) return sensors

        val originalList = sensors.toMutableList()
        val sortedResult = mutableListOf<RuuviTag>()
        for (sensorId in sortingOrder) {
            val sensor = originalList.firstOrNull{it.id == sensorId}
            if (sensor != null) {
                sortedResult.add(sensor)
                originalList.remove(sensor)
            }
        }
        sortedResult.addAll(originalList)


        return sortedResult
    }

    fun newOrder(sensorIds: List<String>) {
        saveListOfSortedSensors(sensorIds)
        for (sens in sensorIds) {
            Timber.d("dragGestureHandler - newOrder $sens")
        }
        Timber.d("dragGestureHandler - newOrder =========================")
    }

    fun getListOfSortedSensors(): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        val sortedSensors = preferences.getSortedSensors()
        Timber.d("Preferences order: $sortedSensors")
        if (sortedSensors.isEmpty()) {
            return emptyList()
        } else {
            return Gson().fromJson(sortedSensors, listType)
        }
    }

    fun saveListOfSortedSensors(sortedSensors: List<String>) {
        preferences.setSortedSensors(Gson().toJson(sortedSensors))
    }
}