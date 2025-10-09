package com.ruuvi.station.dfu.ui

import kotlinx.serialization.Serializable

interface DfuRoute

@Serializable
object UpdateTag: DfuRoute

@Serializable
object UpdateAir: DfuRoute

@Serializable
object UpdateAirDownload: DfuRoute

@Serializable
object UpdateAirInstructions: DfuRoute

@Serializable
object UpdateAirUploadFirmware: DfuRoute

@Serializable
object UpdateAirSuccess: DfuRoute