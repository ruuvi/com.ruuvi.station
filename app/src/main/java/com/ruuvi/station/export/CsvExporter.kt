package com.ruuvi.station.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ruuvi.station.R
import com.ruuvi.station.util.extensions.prepareFilename
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

class CsvExporter(
    private val context: Context,
    private val exportDataPreparator: ExportDataPreparator
) {

    fun toCsv(sensorId: String): Uri? {
        val exportData = exportDataPreparator.prepareExportData(sensorId)
        if (exportData == null) {
            Toast.makeText(context, context.getString(R.string.export_csv_failed), Toast.LENGTH_SHORT).show()
            return null
        }

        val cacheDir = File(context.cacheDir.path + "/export/")
        cacheDir.mkdirs()

        val filenameDate = SimpleDateFormat("yyyyMMdd").format(Date())
        val filenameTime = SimpleDateFormat("HHmmssZ").format(Date())
        val sensorName = if (exportData.sensorName.isNullOrEmpty()) {
            sensorId
        } else {
            exportData.sensorName.prepareFilename()
        }

        val filename = "$cacheDir/${sensorName}_${filenameDate}T${filenameTime}.csv"
        val csvFile = File(filename)

        try {
            if (!csvFile.exists()) csvFile.createNewFile()

            FileWriter(csvFile.absolutePath).use { fileWriter ->
                // Write header row
                exportData.columns.forEachIndexed { index, column ->
                    fileWriter.append(escapeCsvValue(column.name))
                    if (index < exportData.columns.size - 1) {
                        fileWriter.append(',')
                    }
                }
                fileWriter.append('\n')

                // Write data rows
                exportData.rows.forEach { row ->
                    row.forEachIndexed { index, value ->
                        val stringValue = value?.toString() ?: ""
                        fileWriter.append(escapeCsvValue(stringValue))
                        if (index < row.size - 1) {
                            fileWriter.append(',')
                        }
                    }
                    fileWriter.append('\n')
                }

                fileWriter.flush()
            }

            Toast.makeText(context, context.getString(R.string.export_csv_created), Toast.LENGTH_SHORT).show()
            return FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", csvFile)
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.export_csv_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return null
        }
    }

    private fun escapeCsvValue(value: String): String {
        if (value.any { it in ",\"\n\r" }) {
            return "\"${value.replace("\"", "\"\"")}\""
        }
        return value
    }

}