package com.ruuvi.station.alarm.domain

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.equalsEpsilon
import com.ruuvi.station.util.extensions.isInteger
import com.ruuvi.station.util.extensions.round

class AlarmsInteractor(
    private val context: Context,
    private val tagRepository: TagRepository,
    private val alarmRepository: AlarmRepository,
    private val unitsConverter: UnitsConverter,
    private val networkInteractor: RuuviNetworkInteractor,
    private val alarmCheckInteractor: AlarmCheckInteractor,
    ) {

    fun getPossibleRange(type: AlarmType): ClosedFloatingPointRange<Float> {
        return when (type) {
            AlarmType.TEMPERATURE -> {
                val first = unitsConverter.getTemperatureValue(type.possibleRange.first.toDouble()).toFloat()
                val last = unitsConverter.getTemperatureValue(type.possibleRange.last.toDouble()).toFloat()
                first..last
            }
            AlarmType.PRESSURE -> {
                val first = unitsConverter.getPressureValue(type.possibleRange.first.toDouble()).toFloat()
                val last = unitsConverter.getPressureValue(type.possibleRange.last.toDouble()).toFloat()
                first..last
            }
            else -> type.possibleRange.first.toFloat()..type.possibleRange.last.toFloat()
        }
    }

    fun getRangeValue(type: AlarmType, value: Float): Float {
        return when (type) {
            AlarmType.TEMPERATURE -> unitsConverter.getTemperatureValue(value.toDouble()).toFloat()
            AlarmType.PRESSURE -> unitsConverter.getPressureValue(value.toDouble()).toFloat()
            else -> value
        }
    }

    fun getSavableValue(type: AlarmType, value: Float): Double {
        return getSavableValue(type, value.toDouble())
    }

    fun getSavableValue(type: AlarmType, value: Double): Double {
        return when (type) {
            AlarmType.TEMPERATURE -> unitsConverter.getTemperatureCelsiusValue(value).round(4)
            AlarmType.PRESSURE -> unitsConverter.getPressurePascalValue(value)
            else -> value
        }
    }

    fun getDisplayValue(value: Float): String {
        if (value.isInteger(0.009f)) {
            return getDisplayApproximateValue(value)
        } else {
            return getDisplayPreciseValue(value)
        }
    }

    fun getDisplayPreciseValue(value: Float): String {
        if (value.equalsEpsilon(value.round(1), 0.0001f)) {
            return String.format("%1$,.1f", value)
        } else {
            return String.format("%1$,.2f", value)
        }
    }

    fun getDisplayApproximateValue(value: Float): String {
        return value.round(0).toInt().toString()
    }

    fun getAvailableAlarmTypesForSensor(sensor: RuuviTag?): Set<AlarmType> {
        return if (sensor != null) {
            val alarmTypes = mutableSetOf<AlarmType>()
            if (sensor.latestMeasurement?.temperatureValue != null) alarmTypes.add(AlarmType.TEMPERATURE)
            if (sensor.latestMeasurement?.rssiValue != null) alarmTypes.add(AlarmType.RSSI)
            if (sensor.latestMeasurement?.humidityValue != null) alarmTypes.add(AlarmType.HUMIDITY)
            if (sensor.latestMeasurement?.pressureValue != null) alarmTypes.add(AlarmType.PRESSURE)
            if (sensor.latestMeasurement?.movementValue != null) alarmTypes.add(AlarmType.MOVEMENT)
            alarmTypes
        } else {
            emptySet()
        }
    }

    fun getAlarmsForSensor(sensorId: String): List<AlarmItemState> {
        val sensor = tagRepository.getFavoriteSensorById(sensorId)
        val alarmTypes = getAvailableAlarmTypesForSensor(sensor)
        val dbAlarms = alarmRepository.getForSensor(sensorId)
        val alarmItems: MutableList<AlarmItemState> = mutableListOf()

        for (alarmType in alarmTypes) {
            val dbAlarm = dbAlarms.firstOrNull { it.alarmType == alarmType }
            if (dbAlarm != null && sensor != null) {
                alarmItems.add(AlarmItemState.getStateForDbAlarm(dbAlarm, this).also {
                    it.triggered = alarmCheckInteractor.checkAlarm(sensor, dbAlarm)
                })
            } else {
                alarmItems.add(AlarmItemState.getDefaultState(sensorId, alarmType, this))
            }
        }
        return alarmItems
    }

    fun getAlarmTitle(alarmType: AlarmType): String {
        return when (alarmType) {
            AlarmType.TEMPERATURE -> context.getString(R.string.temperature_with_unit, unitsConverter.getTemperatureUnitString())
            AlarmType.HUMIDITY -> context.getString(R.string.humidity_with_unit, context.getString(R.string.humidity_relative_unit))
            AlarmType.PRESSURE -> context.getString(R.string.pressure_with_unit, unitsConverter.getPressureUnitString())
            AlarmType.RSSI -> context.getString(R.string.signal_strength_dbm)
            AlarmType.MOVEMENT -> context.getString(R.string.alert_movement)
        }
    }

    fun getAlarmUnit(alarmType: AlarmType): String {
        return when (alarmType) {
            AlarmType.TEMPERATURE -> unitsConverter.getTemperatureUnitString()
            AlarmType.HUMIDITY -> context.getString(R.string.humidity_relative_unit)
            AlarmType.PRESSURE -> unitsConverter.getPressureUnitString()
            AlarmType.RSSI -> context.getString(R.string.signal_unit)
            AlarmType.MOVEMENT -> context.getString(R.string.alert_movement)
        }
    }

    fun saveAlarm(state: AlarmItemState) {
        val alarm = alarmRepository.upsertAlarm(
            sensorId = state.sensorId,
            min = state.min,
            max = state.max,
            type = state.type.value,
            enabled = state.isEnabled,
            description = state.customDescription,
            mutedTill = state.mutedTill
        )
        networkInteractor.setAlert(alarm)
    }
}