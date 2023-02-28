package com.ruuvi.station.network.data.response

typealias DeleteAccountResponse = RuuviNetworkResponse<DeleteAccountResponseBody>

data class DeleteAccountResponseBody (
    val email: String
)