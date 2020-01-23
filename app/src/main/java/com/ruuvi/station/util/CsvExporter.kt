package com.ruuvi.station.util

import android.content.Context
import android.content.Intent
import android.support.v4.content.FileProvider
import android.widget.Toast
import com.ruuvi.station.database.RuuviTagRepository
import com.ruuvi.station.model.TagSensorReading
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(val context: Context) {
    fun toCsv(tagId: String) {
        val tag = RuuviTagRepository.get(tagId)
        val readings = TagSensorReading.getForTag(tagId)
        val cacheDir = File(context.cacheDir.path + "/export/")
        cacheDir.mkdirs()
        val csvFile = File.createTempFile(
                tag?.id + "_" + Date().time + "_",
                ".csv",
                cacheDir
        )
        var fileWriter: FileWriter?

        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        try {
            fileWriter = FileWriter(csvFile.absolutePath)

            fileWriter.append("timestamp,temperature,humidity,pressure,rssi")
            if (tag?.dataFormat == 3 || tag?.dataFormat == 5) fileWriter.append(",acceleration x,acceleration y,acceleration z,voltage")
            if (tag?.dataFormat == 5) fileWriter.append(",movement counter,measurement sequence number")
            fileWriter.append('\n')


            readings.forEach {
                fileWriter.append(df.format(it.createdAt))
                fileWriter.append(',')
                fileWriter.append(it.temperature.toString())
                fileWriter.append(',')
                fileWriter.append(it.humidity.toString())
                fileWriter.append(',')
                fileWriter.append(it.pressure.toString())
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
        context.startActivity(Intent.createChooser(sendIntent, "RuuviTagEntity "+ tag?.id +" csv export"))
    }
}