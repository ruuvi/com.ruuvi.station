package com.ruuvi.station.database.domain.converter

import com.raizlabs.android.dbflow.converter.TypeConverter
import com.ruuvi.station.database.model.NetworkRequestStatus

@com.raizlabs.android.dbflow.annotation.TypeConverter
class NetworkRequestStatusConverter(): TypeConverter<Int, NetworkRequestStatus>() {
    override fun getDBValue(model: NetworkRequestStatus): Int = model.code

    override fun getModelValue(data: Int): NetworkRequestStatus = NetworkRequestStatus.getById(data)
}