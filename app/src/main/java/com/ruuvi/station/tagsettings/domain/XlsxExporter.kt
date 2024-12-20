package com.ruuvi.station.tagsettings.domain

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.extensions.prepareFilename
import uk.co.spudsoft.xlsx.ColumnDefinition
import uk.co.spudsoft.xlsx.TableDefinition
import uk.co.spudsoft.xlsx.XlsxWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class XlsxExporter (
    private val context: Context,
    private val repository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val unitsConverter: UnitsConverter
){

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun exportToXlsx(sensorId: String): Uri? {
        val tag = repository.getTagById(sensorId)
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)

        val readings = sensorHistoryRepository.getHistory(sensorId, GlobalSettings.historyLengthHours)
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
            "$cacheDir/${tag?.id}_${filenameDate}T${filenameTime}.xlsx"
        } else {
            "$cacheDir/${sensorName}_${filenameDate}T${filenameTime}.xlsx"
        }

        val filePath = File(filename)
        val dataFormat = tag?.dataFormat ?: 0
        try {
            FileOutputStream(filePath).use { fos ->
                val defn = TableDefinition(
                    null,
                    tag?.displayName(),
                    context.getString(R.string.app_name),
                    true,
                    true,
                    null,
                    null,
                    null,
                    null,
                    null,
                    getStandardColumnsDefns(dataFormat)
                )
                XlsxWriter(defn).use { writer ->
                    writer.startFile(fos)
                    for (reading in readings) {
                        writer.outputRow(
                            getDataRow(dataFormat = dataFormat, reading = reading)
                        )
                    }
                }
            }

            return FileProvider.getUriForFile(context, "com.ruuvi.station.fileprovider", filePath)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun getStandardColumnsDefns(dataFormat: Int): List<ColumnDefinition> {
        val humidityUnit = " (${unitsConverter.getHumidityUnitString()})"
        val pressureUnit = " (${unitsConverter.getPressureUnitString()})"
        val columns = mutableListOf(
            ColumnDefinition(context.getString(R.string.date), null, 20.0),
            ColumnDefinition(context.getString(R.string.temperature_with_unit, unitsConverter.getTemperatureUnitString()), null, null),
            ColumnDefinition(context.getString(R.string.humidity) + humidityUnit, null, null),
            ColumnDefinition(context.getString(R.string.pressure) + pressureUnit, null, null),
            ColumnDefinition("RSSI (dBm)", null, null)
        )
        if (dataFormat == 3 || dataFormat == 5) {
            val accelerationUnit = " (${context.getString(R.string.acceleration_unit)})"
            val voltageUnit = " (${context.getString(R.string.voltage_unit)})"
            columns.add(ColumnDefinition(context.getString(R.string.acceleration_x) + accelerationUnit, null, null))
            columns.add(ColumnDefinition(context.getString(R.string.acceleration_y) + accelerationUnit, null, null))
            columns.add(ColumnDefinition(context.getString(R.string.acceleration_z) + accelerationUnit, null, null))
            columns.add(ColumnDefinition(context.getString(R.string.battery_voltage) + voltageUnit, null, null))
        }
        if (dataFormat == 5) {
            val movementsUnit = " (${context.getString(R.string.movements)})"
            val txPowerUnit = " (${context.getString(R.string.signal_unit)})"
            columns.add(ColumnDefinition(context.getString(R.string.movement_counter) + movementsUnit, null, null))
            columns.add(ColumnDefinition(context.getString(R.string.measurement_sequence_number), null, null))
            columns.add(ColumnDefinition(context.getString(R.string.tx_power) + txPowerUnit, null, null))
        }

        return columns
    }

    private fun getDataRow(dataFormat: Int, reading: TagSensorReading): List<Any?> {
        val dataRow = mutableListOf<Any?>(
            dateFormat.format(reading.createdAt),
            reading.temperature?.let { unitsConverter.getTemperatureValue(it) },
            reading.humidity?.let { unitsConverter.getHumidityValue(it, reading.temperature) },
            reading.pressure?.let { unitsConverter.getPressureValue(it) },
            reading.rssi
        )
        if (dataFormat == 3 || dataFormat == 5) {
            dataRow.add(reading.accelX)
            dataRow.add(reading.accelY)
            dataRow.add(reading.accelZ)
            dataRow.add(reading.voltage)
        }
        if (dataFormat == 5) {
            dataRow.add(reading.movementCounter)
            dataRow.add(reading.measurementSequenceNumber)
            dataRow.add(reading.txPower)
        }
        return dataRow
    }
}