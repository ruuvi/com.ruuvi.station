package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.QueryModel
import com.raizlabs.android.dbflow.structure.BaseQueryModel
import com.ruuvi.station.database.domain.LocalDatabase
import java.util.*

@QueryModel(database = LocalDatabase::class)
data class FavouriteSensorQuery(
    @Column
    var id: String = "",
    @Column
    var name: String? = null,
    @Column
    var rssi: Int = 0,
    @Column
    var temperature: Double = 0.0,
    @Column
    var humidity: Double? = null,
    @Column
    var pressure: Double? = null,
    @Column
    var movementCounter: Int = 0,
    @Column
    var defaultBackground: Int = 0,
    @Column
    var userBackground: String? = null,
    @Column
    var networkBackground: String? = null,
    @Column
    var dataFormat: Int = 0,
    @Column
    var updateAt: Date? = null,
    @Column
    var lastSync: Date? = null,
    @Column
    var networkLastSync: Date? = null,
    @Column
    var connectable: Boolean = false,
): BaseQueryModel() {
    companion object {
        val queryFields = arrayOf(
            SensorSettings_Table.id.withTable(),
            SensorSettings_Table.name.withTable(),
            RuuviTagEntity_Table.rssi.withTable(),
            RuuviTagEntity_Table.temperature.withTable(),
            RuuviTagEntity_Table.humidity.withTable(),
            RuuviTagEntity_Table.pressure.withTable(),
            RuuviTagEntity_Table.movementCounter.withTable(),
            SensorSettings_Table.defaultBackground.withTable(),
            SensorSettings_Table.userBackground.withTable(),
            SensorSettings_Table.networkBackground.withTable(),
            RuuviTagEntity_Table.dataFormat.withTable(),
            RuuviTagEntity_Table.updateAt.withTable(),
            SensorSettings_Table.lastSync.withTable(),
            SensorSettings_Table.networkLastSync.withTable(),
            RuuviTagEntity_Table.connectable.withTable()
        )
    }
}