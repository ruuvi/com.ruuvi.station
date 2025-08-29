package com.ruuvi.station.dfu.data

import java.io.File

sealed class DownloadFileStatus{
    data class Progress(val percent: Int): DownloadFileStatus()
    data class Finished(val file: File): DownloadFileStatus()
    data class Failed(val error: String): DownloadFileStatus()
}
