package com.ruuvi.station.network.data.response

typealias GetSubscriptionResponse = RuuviNetworkResponse<GetSubscriptionResponseBody>

data class GetSubscriptionResponseBody(
    val subscriptions: List<SubscriptionInfo>
)

data class SubscriptionInfo(
    val validDays: Int,
    val maxClaims: Int,
    val maxShares: Int,
    val maxSharesPerSensor: Int,
    val maxHistoryDays: Int,
    val maxResolutionMinutes: Int,
    val subscriptionName: String,
    val claimCode: String,
    val creatorId: String,
    val isActive: Int,
    val startTime: Long,
    val endTime: Long
)