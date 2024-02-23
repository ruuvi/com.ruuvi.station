package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.QueryModel
import com.raizlabs.android.dbflow.structure.BaseQueryModel
import com.ruuvi.station.database.domain.LocalDatabase
import java.util.*

@QueryModel(database = LocalDatabase::class)
data class FavouriteSensorQuery(
    // SensorSettings
    @Column
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
    // RuuviTagEntity
    @Column
    var latestId: String? = null,
    @Column
    var rssi: Int = 0,
    @Column
    var temperature: Double = 0.0,
    @Column
    var humidity: Double? = null,
    @Column
    var pressure: Double? = null,
    @Column
    var accelX: Double = 0.0,
    @Column
    var accelY: Double = 0.0,
    @Column
    var accelZ: Double = 0.0,
    @Column
    var voltage: Double = 0.0,
    @Column
    var updateAt: Date? = null,
    @Column
    var dataFormat: Int = 0,
    @Column
    var txPower: Double = 0.0,
    @Column
    var movementCounter: Int? = null,
    @Column
    var measurementSequenceNumber: Int = 0,
    @Column
    var connectable: Boolean = false,
): BaseQueryModel() {
    companion object {
        val queryFields = arrayOf(
            SensorSettings_Table.id.withTable(),
            SensorSettings_Table.createDate.withTable(),
            SensorSettings_Table.name.withTable(),
            SensorSettings_Table.defaultBackground.withTable(),
            SensorSettings_Table.userBackground.withTable(),
            SensorSettings_Table.networkBackground.withTable(),
            SensorSettings_Table.humidityOffset.withTable(),
            SensorSettings_Table.humidityOffsetDate.withTable(),
            SensorSettings_Table.temperatureOffset.withTable(),
            SensorSettings_Table.temperatureOffsetDate.withTable(),
            SensorSettings_Table.pressureOffset.withTable(),
            SensorSettings_Table.pressureOffsetDate.withTable(),
            SensorSettings_Table.owner.withTable(),
            SensorSettings_Table.subscriptionName.withTable(),
            SensorSettings_Table.lastSync.withTable(),
            SensorSettings_Table.networkLastSync.withTable(),
            SensorSettings_Table.networkSensor.withTable(),
            SensorSettings_Table.firmware.withTable(),
            RuuviTagEntity_Table.id.withTable().`as`("latestId"),
            RuuviTagEntity_Table.rssi.withTable(),
            RuuviTagEntity_Table.temperature.withTable(),
            RuuviTagEntity_Table.humidity.withTable(),
            RuuviTagEntity_Table.pressure.withTable(),
            RuuviTagEntity_Table.accelX.withTable(),
            RuuviTagEntity_Table.accelY.withTable(),
            RuuviTagEntity_Table.accelZ.withTable(),
            RuuviTagEntity_Table.voltage.withTable(),
            RuuviTagEntity_Table.updateAt.withTable(),
            RuuviTagEntity_Table.dataFormat.withTable(),
            RuuviTagEntity_Table.txPower.withTable(),
            RuuviTagEntity_Table.movementCounter.withTable(),
            RuuviTagEntity_Table.measurementSequenceNumber.withTable(),
            RuuviTagEntity_Table.connectable.withTable()
        )
    }
}