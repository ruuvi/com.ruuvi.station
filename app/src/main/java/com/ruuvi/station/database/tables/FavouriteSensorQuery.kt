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
): BaseQueryModel()