package com.ruuvi.station.dfu.data

data class LatestReleaseResponse(
    val name: String,
    val tag_name: String,
    val assets: List<ReleaseAssets>
)

data class ReleaseAssets(
    val name: String,
    val browser_download_url: String
)