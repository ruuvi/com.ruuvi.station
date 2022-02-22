package com.ruuvi.station.dataforwarding.domain

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import com.ruuvi.station.app.preferences.PreferencesRepository
import com.ruuvi.station.bluetooth.domain.LocationInteractor
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.SensorSettings
import com.ruuvi.station.dataforwarding.data.ScanEvent
import com.ruuvi.station.dataforwarding.data.SensorInfo
import timber.log.Timber

class EventFactory (
    private val context: Context,
    private val locationInteractor: LocationInteractor,
    private val preferences: PreferencesRepository
) {
    private val batteryManager: BatteryManager by lazy {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    }

    fun createEvent(tagEntity: RuuviTagEntity, sensorSettings: SensorSettings): ScanEvent {
        val deviceId = preferences.getDeviceId()
        val shouldIncludeLocation = preferences.getDataForwardingLocationEnabled()
        val location = if (shouldIncludeLocation) locationInteractor.getLocation() else null
        val scanEvent = ScanEvent(deviceId, location, getBatteryLevel())
        scanEvent.tags.add(SensorInfo.createEvent(tagEntity, sensorSettings))
        return scanEvent
    }

    fun createTestEvent(): ScanEvent {
        val deviceId = preferences.getDeviceId()
        val shouldIncludeLocation = preferences.getDataForwardingLocationEnabled()
        val location = if (shouldIncludeLocation) locationInteractor.getLocation() else null
        return ScanEvent(deviceId, location, getBatteryLevel())
    }

    private fun getBatteryLevel(): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
        return null
    }
}