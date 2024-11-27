package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.*
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import java.util.*

@Table (
    database = LocalDatabase::class,
    useBooleanGetterSetters = false
)
data class SensorSettings(
    @Column
    @PrimaryKey
    var id: String = "",
    @Column
    var createDate: Date? = null,
    @Column
    var name: String? = null,
    @Column
    var defaultBackground: Int = 0,
    @Column
    var userBackground: String? = null,
    @Column
    var networkBackground: String? = null,
    @Column
    var humidityOffset: Double? = null,
    @Column
    var humidityOffsetDate: Date? = null,
    @Column
    var temperatureOffset: Double? = null,
    @Column
    var temperatureOffsetDate: Date? = null,
    @Column
    var pressureOffset: Double? = null,
    @Column
    var pressureOffsetDate: Date? = null,
    @Column
    var owner: String? = null,
    @Column
    var subscriptionName: String? = null,
    @Column
    var lastSync: Date? = null,
    @Column
    var networkLastSync: Date? = null,
    @Column
    var networkSensor: Boolean = false,
    @Column
    var firmware: String? = null,
    @Column
    var networkHistoryLastSync: Date? = null,
    @Column
    var canShare: Boolean? = null,
): BaseModel() {
    val displayName get() = if (name.isNullOrEmpty()) id else name.toString()

    fun updateFromNetwork(sensor: SensorsDenseInfo) {
        name = sensor.name
        owner = sensor.owner.lowercase()
        canShare = sensor.canShare
        humidityOffset = sensor.offsetHumidity
        pressureOffset = sensor.offsetPressure
        temperatureOffset = sensor.offsetTemperature
        networkSensor = true
        subscriptionName = sensor.subscription?.subscriptionName
        update()
    }
}