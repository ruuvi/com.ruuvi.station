package com.ruuvi.station.alarm.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import kotlin.math.round

class AlarmsInteractor(
    private val context: Context,
    private val tagRepository: TagRepository,
    private val alarmRepository: AlarmRepository,
    private val unitsConverter: UnitsConverter,
    private val networkInteractor: RuuviNetworkInteractor
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

    fun getAlarmsForSensor(sensorId: String): List<AlarmItemState> {
        val alarmTypes = getAvailableAlarmTypesForSensor(sensorId)
        val dbAlarms = alarmRepository.getForSensor(sensorId)
        val alarmItems: MutableList<AlarmItemState> = mutableListOf()

        for (alarmType in alarmTypes) {
            val dbAlarm = dbAlarms.firstOrNull { it.alarmType == alarmType }
            if (dbAlarm != null) {
                alarmItems.add(AlarmItemState(dbAlarm))
            } else {
                alarmItems.add(AlarmItemState(sensorId, alarmType))
            }
        }
        return alarmItems
    }

    fun getAlarmTitle(alarmType: AlarmType): String {
        return when (alarmType) {
            AlarmType.TEMPERATURE -> context.getString(R.string.temperature_with_unit, unitsConverter.getTemperatureUnitString())
            AlarmType.HUMIDITY -> context.getString(R.string.humidity_with_unit, context.getString(R.string.humidity_relative_unit))
            AlarmType.PRESSURE -> context.getString(R.string.pressure_with_unit, unitsConverter.getPressureUnitString())
            AlarmType.RSSI -> context.getString(R.string.rssi)
            AlarmType.MOVEMENT -> context.getString(R.string.alert_movement)
        }
    }

    fun getDisplayValue(alarmType: AlarmType, value: Int): Int {
        return when (alarmType) {
            AlarmType.TEMPERATURE -> {
                round(unitsConverter.getTemperatureValue(value.toDouble())).toInt()
            }
            AlarmType.PRESSURE -> {
                round(unitsConverter.getPressureValue(value.toDouble())).toInt()
            }
            else -> value
        }
    }

    fun saveAlarm(state: AlarmItemState) {
        val alarm = alarmRepository.upsertAlarm(
            sensorId = state.sensorId,
            low = state.low.toInt(),
            high = state.high.toInt(),
            first = state.low,
            last = state.high,
            type = state.type.value,
            enabled = state.isEnabled,
            description = state.customDescription
        )
        networkInteractor.setAlert(alarm)
    }
}