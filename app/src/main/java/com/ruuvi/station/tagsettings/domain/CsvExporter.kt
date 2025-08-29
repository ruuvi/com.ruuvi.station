package com.ruuvi.station.tagsettings.domain

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.isAir
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.util.extensions.prepareFilename
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

    fun toCsv(tagId: String): Uri? {
        val tag = repository.getTagById(tagId)
        val sensorSettings = sensorSettingsRepository.getSensorSettings(tagId)

        val readings = sensorHistoryRepository.getHistory(tagId, GlobalSettings.historyLengthHours)
            .map {
                it.copy(
                    temperature = it.temperature?.let { temperature ->
                        temperature + (sensorSettings?.temperatureOffset ?: 0.0)
                    },
                    humidity = it.humidity?.let { humidity ->
                        humidity + (sensorSettings?.humidityOffset ?: 0.0)
                    },
                    pressure = it.pressure?.let { pressure ->
                        pressure + (sensorSettings?.pressureOffset ?: 0.0)
                    }
                )
            }
        val cacheDir = File(context.cacheDir.path + "/export/")
        cacheDir.mkdirs()

        val filenameDate = SimpleDateFormat("yyyyMMdd").format(Date())
        val filenameTime = SimpleDateFormat("HHmmssZ").format(Date())
        val sensorName = sensorSettings?.name?.prepareFilename()

        val filename = if (sensorName.isNullOrEmpty()) {
            "$cacheDir/${tag?.id}_${filenameDate}T${filenameTime}.csv"
        } else {
            "$cacheDir/${sensorName}_${filenameDate}T${filenameTime}.csv"
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
                0xE0 -> fileWriter.append(
                    context.getString(
                        R.string.export_csv_header_formatE0,
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
                if (tag?.isAir() == true) {
                    fileWriter.append(AQI.getAQI(reading.pm25, reading.co2).score?.let { it.roundHalfUp(1).toString() } ?: nullValue)
                    fileWriter.append(',')
                }
                fileWriter.append(reading.temperature?. let { unitsConverter.getTemperatureValue(it).toString() } ?: nullValue)
                fileWriter.append(',')
                fileWriter.append(reading.humidity?.let { unitsConverter.getHumidityValue(it, reading.temperature).toString() } ?: nullValue)
                fileWriter.append(',')
                fileWriter.append(reading.pressure?.let { unitsConverter.getPressureValue(it).toString() } ?: nullValue)
                fileWriter.append(',')
                if (tag?.isAir() == true) {
                    fileWriter.append(reading.luminosity?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.dBaAvg?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.dBaPeak?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.co2?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.voc?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.nox?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.pm1?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.pm25?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.pm4?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.pm10?.toString() ?: nullValue)
                    fileWriter.append(',')
                }
                fileWriter.append(reading.rssi?.toString() ?: nullValue)
                if (tag?.dataFormat == 3 || tag?.dataFormat == 5 || tag?.isAir() == true) {
                    fileWriter.append(',')
                    fileWriter.append(reading.accelX?.let { reading.accelX.toString() } ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.accelY?.let { reading.accelY.toString() } ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.accelZ?.let { reading.accelZ.toString() } ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.voltage?.toString() ?: nullValue)
                }
                if (tag?.dataFormat == 5 || tag?.isAir() == true) {
                    fileWriter.append(',')
                    fileWriter.append(reading.movementCounter?.let { reading.movementCounter.toString() } ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.measurementSequenceNumber?.toString() ?: nullValue)
                    fileWriter.append(',')
                    fileWriter.append(reading.txPower?.toInt()?.toString() ?: nullValue)
                }
                fileWriter.append('\n')
            }

            fileWriter.flush()
            fileWriter.close()
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.export_csv_failed), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
            return null
        }

        Toast.makeText(context, context.getString(R.string.export_csv_created), Toast.LENGTH_SHORT).show()
        return FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", csvFile)
    }

    companion object {
        private const val nullValue = ""
    }
}