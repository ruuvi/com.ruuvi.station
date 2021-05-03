package com.ruuvi.station.database.tables

import com.google.gson.Gson
import com.raizlabs.android.dbflow.annotation.Column
import com.raizlabs.android.dbflow.annotation.PrimaryKey
import com.raizlabs.android.dbflow.annotation.Table
import com.ruuvi.station.database.domain.LocalDatabase
import com.ruuvi.station.database.domain.converter.NetworkRequestStatusConverter
import com.ruuvi.station.database.domain.converter.NetworkRequestTypeConverter
import com.ruuvi.station.database.model.NetworkRequestStatus
import com.ruuvi.station.database.model.NetworkRequestType
import java.util.*

@Table(
    database = LocalDatabase::class,
    useBooleanGetterSetters = false
)
data class NetworkRequest(
    @Column
    @PrimaryKey(autoincrement = true)
    var id: Int = 0,
    @Column(typeConverter = NetworkRequestTypeConverter::class)
    var type: NetworkRequestType = NetworkRequestType.SETTING,
    @Column
    var key: String = "",
    @Column
    var requestDate: Date = Date(),
    @Column
    var successDate: Date? = null,
    @Column
    var attempts: Int = 0,
    @Column
    var data: String = "",
    @Column(typeConverter = NetworkRequestStatusConverter::class)
    var status: NetworkRequestStatus = NetworkRequestStatus.READY
) {
    constructor (type: NetworkRequestType, key: String, request: Any): this(
        type = type,
        key = key,
        data = Gson().toJson(request)
    )
}