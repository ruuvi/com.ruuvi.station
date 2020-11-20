package com.ruuvi.station.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ruuvi.station.database.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.units.domain.UnitsConverter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(
    private val context: Context,
    private val repository: TagRepository,
    private val unitsConverter: UnitsConverter
) {

    fun toCsv(tagId: String) {
        val tag = repository.getTagById(tagId)
        val readings = TagSensorReading.getForTag(tagId)
        val cacheDir = File(context.cacheDir.path + "/export/")
        cacheDir.mkdirs()

        val filenameDateFormat = SimpleDateFormat("yyMMdd-hhmm")
        val filenameDate = filenameDateFormat.format(Date())

        val filename = if (tag?.name.isNullOrEmpty()) {
            "$cacheDir/${tag?.id}_${filenameDate}.csv"
        } else {
            "$cacheDir/${tag?.name}_${filenameDate}.csv"
        }

        val csvFile = File(filename)
        if (!csvFile.exists()) csvFile.createNewFile()

        try {
            var fileWriter = FileWriter(csvFile.absolutePath)

            fileWriter.append("timestamp,temperature (${unitsConverter.getTemperatureUnitString()})," +
                "humidity (${unitsConverter.getHumidityUnitString()})," +
                "pressure (${unitsConverter.getPressureUnitString()}),rssi")
            if (tag?.dataFormat == 3 || tag?.dataFormat == 5) fileWriter.append(",acceleration x,acceleration y,acceleration z,voltage")
            if (tag?.dataFormat == 5) fileWriter.append(",movement counter,measurement sequence number")
            fileWriter.append('\n')

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())

            readings.forEach {
                fileWriter.append(dateFormat.format(it.createdAt))
                fileWriter.append(',')
                fileWriter.append(unitsConverter.getTemperatureValue(it.temperature).toString())
                fileWriter.append(',')
                fileWriter.append(unitsConverter.getHumidityValue(it.humidity, it.temperature).toString())
                fileWriter.append(',')
                fileWriter.append(unitsConverter.getPressureValue(it.pressure).toString())
                fileWriter.append(',')
                fileWriter.append(it.rssi.toString())
                if (tag?.dataFormat == 3 || tag?.dataFormat == 5) {
                    fileWriter.append(',')
                    fileWriter.append(it.accelX.toString())
                    fileWriter.append(',')
                    fileWriter.append(it.accelY.toString())
                    fileWriter.append(',')
                    fileWriter.append(it.accelZ.toString())
                    fileWriter.append(',')
                    fileWriter.append(it.voltage.toString())
                }
                if (tag?.dataFormat == 5) {
                    fileWriter.append(',')
                    fileWriter.append(it.movementCounter.toString())
                    fileWriter.append(',')
                    fileWriter.append(it.measurementSequenceNumber.toString())
                }
                fileWriter.append('\n')
            }

            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to create CSV file", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return
        }

        Toast.makeText(context, ".csv created, opening share menu", Toast.LENGTH_SHORT).show()
        val uri = FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", csvFile)

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sendIntent.type = "text/csv"
        sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(Intent.createChooser(sendIntent, "RuuviTagEntity " + tag?.id + " csv export"))
    }
}