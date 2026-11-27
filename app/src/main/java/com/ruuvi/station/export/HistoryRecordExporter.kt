package com.ruuvi.station.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ruuvi.station.history.HistoryWindow
import com.ruuvi.station.history.ProgressiveHistoryRepository
import com.ruuvi.station.util.extensions.prepareFilename
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** One export at a time. A shareable URI exists only after all requested pages and the file close succeed. */
class HistoryRecordExporter(private val context: Context, private val preparator: ExportDataPreparator,
                            private val history: ProgressiveHistoryRepository) {
    private val mutex = Mutex()
    suspend fun export(access: ProgressiveHistoryRepository.Access, window: HistoryWindow, xlsx: Boolean): Uri = withContext(Dispatchers.IO) {
        mutex.withLock {
            val schema = preparator.prepareStreamingExport(access.sensor) ?: error("Sensor unavailable.")
            val directory = File(context.cacheDir, "export").also { check(it.isDirectory || it.mkdirs()) }
            val file = File(directory, "${schema.sensorName.prepareFilename().take(80).ifEmpty { "History" }}_${UUID.randomUUID()}.${if (xlsx) "xlsx" else "csv"}")
            var completed = false
            try {
                if (xlsx) {
                    StreamingHistoryXlsx(file.outputStream().buffered(), schema.columns).use { writer ->
                        history.records(access, window) { page ->
                            currentCoroutineContext().ensureActive()
                            page.forEach { writer.writeRow(schema.row(access.calibrate(it))) }
                        }
                    }
                } else file.bufferedWriter(Charsets.UTF_8).use { writer ->
                    writeHistoryCsvRow(writer, schema.columns.map { it.name })
                    history.records(access, window) { page ->
                        currentCoroutineContext().ensureActive()
                        page.forEach { writeHistoryCsvRow(writer, schema.row(access.calibrate(it))) }
                    }
                }
                currentCoroutineContext().ensureActive()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                completed = true; uri
            } finally { if (!completed) file.delete() }
        }
    }
}
