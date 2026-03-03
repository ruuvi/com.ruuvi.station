package com.ruuvi.station.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.prepareFilename
import uk.co.spudsoft.xlsx.ColumnDefinition
import uk.co.spudsoft.xlsx.TableDefinition
import uk.co.spudsoft.xlsx.XlsxWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date

class XlsxExporter(
    private val context: Context,
    private val exportDataPreparator: ExportDataPreparator
) {

    fun exportToXlsx(sensorId: String): Uri? {
        val exportData = exportDataPreparator.prepareExportData(sensorId) ?: return null

        val cacheDir = File(context.cacheDir.path + "/export/")
        cacheDir.mkdirs()

        val filenameDate = SimpleDateFormat("yyyyMMdd").format(Date())
        val filenameTime = SimpleDateFormat("HHmmssZ").format(Date())
        val sensorName = if (exportData.sensorName.isNullOrEmpty()) {
            sensorId
        } else {
            exportData.sensorName.prepareFilename()
        }

        val filename = "$cacheDir/${sensorName}_${filenameDate}T${filenameTime}.xlsx"
        val filePath = File(filename)

        try {
            FileOutputStream(filePath).use { fos ->
                val columnDefinitions = exportData.columns.map { column ->
                    ColumnDefinition(column.name, null, column.width)
                }

                val tableDefinition = TableDefinition(
                    null,
                    exportData.sensorName,
                    context.getString(R.string.app_name),
                    true,
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                    columnDefinitions
                )

                XlsxWriter(tableDefinition).use { writer ->
                    writer.startFile(fos)
                    for (row in exportData.rows) {
                        writer.outputRow(row)
                    }
                }
            }

            return FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}