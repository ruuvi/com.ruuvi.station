package com.ruuvi.station.network.ui.model

data class ShareOperationStatus(
    val type: ShareOperationType,
    val message: String
)

enum class ShareOperationType {
    SHARING_ERROR,
    SHARING_SUCCESS,
}