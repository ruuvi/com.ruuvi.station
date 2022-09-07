package com.ruuvi.station.alarm.domain

import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.TagRepository

class AlarmsInteractor(
    private val tagRepository: TagRepository,
    private val alarmRepository: AlarmRepository,
) {

    fun getAvailableAlarmTypesForSensor(sensorId: String): Set<AlarmType> {
        val entry = tagRepository.getTagById(sensorId)
        return if (entry != null) {
            val alarmTypes = mutableSetOf(AlarmType.TEMPERATURE, AlarmType.RSSI)
            if (entry.humidity != null) alarmTypes.add(AlarmType.HUMIDITY)
            if (entry.pressure != null) alarmTypes.add(AlarmType.PRESSURE)
            if (entry.movementCounter != null) alarmTypes.add(AlarmType.MOVEMENT)
            alarmTypes
        } else {
            emptySet()
        }
    }

    fun getAlarmsForSensor(sensorId: String): MutableList<AlarmElement> {
        val alarmTypes = getAvailableAlarmTypesForSensor(sensorId)
        val dbAlarms = alarmRepository.getForSensor(sensorId)
        val alarmElements: MutableList<AlarmElement> = mutableListOf()

        for (alarmType in alarmTypes) {
            val dbAlarm = dbAlarms.firstOrNull { it.alarmType == alarmType }
            if (dbAlarm != null) {
                alarmElements.add(AlarmElement.getDbAlarmElement(dbAlarm))
            } else {
                alarmElements.add(AlarmElement.getDefaultAlarmElement(sensorId, alarmType))
            }
        }
        return alarmElements
    }
}