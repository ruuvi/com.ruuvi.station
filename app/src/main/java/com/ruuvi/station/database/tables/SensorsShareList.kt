package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.database.domain.LocalDatabase

@Table(
    database = LocalDatabase::class,
    useBooleanGetterSetters = false
)
data class SensorsShareList(
    @PrimaryKey
    @Column
    var sensorId: String = "",
    @PrimaryKey
    @Column
    var userEmail: String = ""
): BaseModel()