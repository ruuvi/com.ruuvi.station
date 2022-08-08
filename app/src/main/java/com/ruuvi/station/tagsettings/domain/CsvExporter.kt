package com.ruuvi.station.tagsettings.domain

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.units.domain.UnitsConverter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(
    private val context: Context,
    private val repository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val unitsConverter: UnitsConverter
) {

    fun toCsv(tagId: String) {
        val tag = repository.getTagById(tagId)
        val readings = sensorHistoryRepository.getHistory(tagId, GlobalSettings.historyLengthDays)
        val cacheDir = File(context.cacheDir.path + "/export/")
        cacheDir.mkdirs()

        val filenameDateFormat = SimpleDateFormat("yyyyMMdd-HHmm")
        val filenameDate = filenameDateFormat.format(Date())
        val sensorSettings = sensorSettingsRepository.getSensorSettings(tagId)

        val filename = if (sensorSettings?.name.isNullOrEmpty()) {
            "$cacheDir/${tag?.id}_${filenameDate}.csv"
        } else {
            "$cacheDir/${sensorSettings?.name}_${filenameDate}.csv"
        }

        val csvFile = File(filename)
        if (!csvFile.exists()) csvFile.createNewFile()

        try {
            var fileWriter = FileWriter(csvFile.absolutePath)

            when (tag?.dataFormat) {
                3 -> fileWriter.append(
                    context.getString(
                        R.string.export_csv_header_format3,
                        unitsConverter.getTemperatureUnitString(),
                        unitsConverter.getHumidityUnitString(),
                        unitsConverter.getPressureUnitString()
                    ))
                5 -> fileWriter.append(
                    context.getString(
                        R.string.export_csv_header_format5,
                        unitsConverter.getTemperatureUnitString(),
                        unitsConverter.getHumidityUnitString(),
                        unitsConverter.getPressureUnitString()
                    ))
                else -> fileWriter.append(
                    context.getString(
                        R.string.export_csv_header_format2_4,
                        unitsConverter.getTemperatureUnitString(),
                        unitsConverter.getHumidityUnitString(),
                        unitsConverter.getPressureUnitString()
                    ))
            }
            fileWriter.append('\n')
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            readings.forEach { reading->
                fileWriter.append(dateFormat.format(reading.createdAt))
                fileWriter.append(',')
                fileWriter.append(unitsConverter.getTemperatureValue(reading.temperature).toString())
                fileWriter.append(',')
                fileWriter.append(reading.humidity?.let { unitsConverter.getHumidityValue(it, reading.temperature).toString() } ?: nullValue)
                fileWriter.append(',')
                fileWriter.append(reading.pressure?.let { unitsConverter.getPressureValue(it).toString() } ?: nullValue)
                fileWriter.append(',')
                fileWriter.append(reading.rssi.toString())
                if (tag?.dataFormat == 3 || tag?.dataFormat == 5) {
                    fileWriter.append(',')
                    fileWriter.append(reading.accelX.toString())
                    fileWriter.append(',')
                    fileWriter.append(reading.accelY.toString())
                    fileWriter.append(',')
                    fileWriter.append(reading.accelZ.toString())
                    fileWriter.append(',')
                    fileWriter.append(reading.voltage.toString())
                }
                if (tag?.dataFormat == 5) {
                    fileWriter.append(',')
                    fileWriter.append(reading.movementCounter.toString())
                    fileWriter.append(',')
                    fileWriter.append(reading.measurementSequenceNumber.toString())
                    fileWriter.append(',')
                    fileWriter.append(reading.txPower.toString())
                }
                fileWriter.append('\n')
            }

            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.export_csv_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return
        }

        Toast.makeText(context, context.getString(R.string.export_csv_created), Toast.LENGTH_SHORT).show()
        val uri = FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", csvFile)

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sendIntent.type = "text/csv"
        sendIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.export_csv_chooser_title, tag?.id)))
    }

    companion object {
        private const val nullValue = ""
    }
}