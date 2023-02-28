package com.ruuvi.station.tagdetails.domain

data class TagDetailsArguments(
    val desiredTag: String? = null,
    val shouldOpenAddView: Boolean = false,
    val showHistory: Boolean = false
)