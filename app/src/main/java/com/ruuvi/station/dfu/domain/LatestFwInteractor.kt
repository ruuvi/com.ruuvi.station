package com.ruuvi.station.dfu.domain

import com.ruuvi.station.dfu.data.DownloadFileStatus
import com.ruuvi.station.dfu.data.LatestReleaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import okhttp3.ResponseBody
import timber.log.Timber
import java.io.File

class LatestFwInteractor(
    private val gitHubRepository: GitHubRepository
) {
    suspend fun funGetLatestFwVersion(): LatestReleaseResponse? {
        val response = gitHubRepository.getLatestFwVersion()
        Timber.d(response.toString())
        return response
    }

    suspend fun downloadFw(fileUrl: String, filename: String, collectStatus: (DownloadFileStatus)->Unit) {
        gitHubRepository.getFile(fileUrl).downloadToFileWithProgress(filename).collect {
            collectStatus(it)
        }
    }
}

fun ResponseBody.downloadToFileWithProgress(filename: String): Flow<DownloadFileStatus> =
    flow {
        emit(DownloadFileStatus.Progress(0))

        // flag to delete file if download errors or is cancelled
        var deleteFile = true
        val file = File(filename)

        try {
            byteStream().use { inputStream ->
                file.outputStream().use { outputStream ->
                    val totalBytes = contentLength()
                    val data = ByteArray(8_192)
                    var progressBytes = 0L

                    while (true) {
                        val bytes = inputStream.read(data)

                        if (bytes == -1) {
                            break
                        }

                        outputStream.channel
                        outputStream.write(data, 0, bytes)
                        progressBytes += bytes

                        emit(DownloadFileStatus.Progress(percent = ((progressBytes * 100) / totalBytes).toInt()))
                    }

                    when {
                        progressBytes < totalBytes ->
                            throw Exception("missing bytes")
                        progressBytes > totalBytes ->
                            throw Exception("too many bytes")
                        else ->
                            deleteFile = false
                    }
                }
            }

            emit(DownloadFileStatus.Finished(file))
        } finally {
            // check if download was successful

            if (deleteFile) {
                file.delete()
            }
        }
    }
        .flowOn(Dispatchers.IO)
        .distinctUntilChanged()
