package com.ruuvi.station.dashboard.domain

import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.tag.domain.RuuviTag

class SensorsSortingInteractor (
    val preferences: PreferencesRepository
){
    fun sortSensors(sensors: List<RuuviTag>): List<RuuviTag> {
        val sortingOrder = preferences.getSortedSensors()
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
        preferences.setSortedSensors(sensorIds)
    }


}