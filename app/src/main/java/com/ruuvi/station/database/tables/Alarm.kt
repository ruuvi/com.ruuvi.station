package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.LocalDatabase
import java.util.*

@Table(
    database = LocalDatabase::class,
    useBooleanGetterSetters = false
)
data class Alarm (
    @Column
    @PrimaryKey(autoincrement = true)
    var id: Int = 0,
    @Column
    var ruuviTagId: String = "",
    @Column
    var low: Int = 0, // Obsolete
    @Column
    var high: Int = 0, // Obsolete
    @Column
    var min: Double = 0.0,
    @Column
    var max: Double = 0.0,
    @Column
    var type: Int = 0,
    @Column
    var enabled: Boolean = false,
    @Column
    var mutedTill: Date? = null,
    @Column
    var customDescription: String = ""
) {
    val alarmType: AlarmType
        get() = AlarmType.getByDbCode(type)

    companion object {
        const val TEMPERATURE = 0
        const val HUMIDITY = 1
        const val PRESSURE = 2
        const val RSSI = 3
        const val MOVEMENT = 4
    }
}