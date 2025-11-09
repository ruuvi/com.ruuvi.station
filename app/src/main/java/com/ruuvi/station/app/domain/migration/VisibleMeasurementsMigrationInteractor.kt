package com.ruuvi.station.app.domain.migration

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.gson.Gson
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.VisibleMeasurementsOrderInteractor
import com.ruuvi.station.tagsettings.domain.TagSettingsInteractor
import com.ruuvi.station.units.model.UnitType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import timber.log.Timber

class VisibleMeasurementsMigrationInteractor(
    val context: Context,
    private val tagRepository: TagRepository,
    private val alarmRepository: AlarmRepository,
    private val networkInteractor: RuuviNetworkInteractor,
    private val tagSettingsInteractor: TagSettingsInteractor,
    private val visibleMeasurementsOrderInteractor: VisibleMeasurementsOrderInteractor,
) {
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    private var migrationDone: Boolean
        get() = preferences.getBoolean(MIGRATION_DONE_KEY, false)
        set(locationEnabled) {
            preferences.edit { putBoolean(MIGRATION_DONE_KEY, locationEnabled) }
        }

    fun migrate() {
        if (!migrationDone) {

            val alarms = alarmRepository.getActiveAlarms()
            for (alarm in alarms) {
                val ruuviTag = tagRepository.getFavoriteSensorById(alarm.ruuviTagId)
                when (alarm.type) {
                    AlarmType.RSSI.value -> {
                        if (ruuviTag?.defaultDisplayOrder == true) {
                            tagSettingsInteractor.setUseDefaultSensorsOrder(
                                alarm.ruuviTagId,
                                false
                            )
                            val displayOrder = visibleMeasurementsOrderInteractor
                                .getDefaultDisplayOrder(ruuviTag)
                                .map { it.getCode() }
                                .toMutableList()
                            displayOrder.add(UnitType.SignalStrengthUnit.SignalDbm.getCode())
                            tagSettingsInteractor.newDisplayOrder(
                                alarm.ruuviTagId,
                                Gson().toJson(displayOrder)
                            )
                        }
                    }

                    AlarmType.PM10.value, AlarmType.PM40.value, AlarmType.PM100.value -> {
                        if (ruuviTag?.defaultDisplayOrder == true) {
                            alarmRepository.disableAlarm(alarm)
                            val flow = networkInteractor.setAlert(alarm)
                            flow?.launchIn(CoroutineScope(Dispatchers.IO))
                        }
                    }
                }
            }

            migrationDone = true
        }
    }

    companion object {
        private const val PREFERENCES_NAME = "migration_visible_measurements"
        private const val MIGRATION_DONE_KEY = "migration_done"
    }
}