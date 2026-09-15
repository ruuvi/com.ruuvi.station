package com.ruuvi.station.database.tables

import com.raizlabs.android.dbflow.annotation.*
import com.raizlabs.android.dbflow.structure.BaseModel
import com.ruuvi.station.database.domain.LocalDatabase

@Table(database = LocalDatabase::class, indexGroups = [IndexGroup(number = 1, name = "HistoryCoverageSensor")])
data class HistoryCoverage(
    @PrimaryKey(autoincrement = true) @Column var id: Long = 0,
    @Index(indexGroups = [1]) @Column var sensorId: String = "",
    @Column var account: String = "",
    @Column var backend: String = "",
    @Column var startMillis: Long = 0,
    @Column var endExclusiveMillis: Long = 0,
    @Column var fetchedAt: Long = 0
) : BaseModel()
