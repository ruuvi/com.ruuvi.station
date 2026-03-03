package com.ruuvi.station.export

import android.content.Context
import com.ruuvi.station.R
import com.ruuvi.station.app.preferences.GlobalSettings
import com.ruuvi.station.bluetooth.util.extensions.roundHalfUp
import com.ruuvi.station.database.domain.SensorHistoryRepository
import com.ruuvi.station.database.domain.SensorSettingsRepository
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.database.tables.RuuviTagEntity
import com.ruuvi.station.database.tables.TagSensorReading
import com.ruuvi.station.tag.domain.RuuviTag.Companion.dataFormatIsAir
import com.ruuvi.station.units.domain.HumidityConverter
import com.ruuvi.station.units.domain.TemperatureConverter.Companion.celsiusToFahrenheit
import com.ruuvi.station.units.domain.TemperatureConverter.Companion.celsiusToKelvin
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.units.domain.aqi.AQI
import com.ruuvi.station.units.model.UnitType
import com.ruuvi.station.units.model.UnitType.HumidityUnit
import java.text.SimpleDateFormat
import java.util.Locale

class ExportDataPreparator(
    private val context: Context,
    private val repository: TagRepository,
    private val sensorHistoryRepository: SensorHistoryRepository,
    private val sensorSettingsRepository: SensorSettingsRepository,
    private val unitsConverter: UnitsConverter
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun prepareExportData(sensorId: String): ExportData? {
        val tag = repository.getTagById(sensorId) ?: return null
        val sensorSettings = sensorSettingsRepository.getSensorSettings(sensorId)
        val sensorName = sensorSettings?.name ?: ""

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

        val dataFormat = tag.dataFormat
        val isAir = dataFormatIsAir(dataFormat)

        val columns = buildColumns(tag, dataFormat, isAir)
        val rows = readings.map { reading -> buildRow(tag, reading, dataFormat, isAir) }

        return ExportData(
            sensorName = sensorName,
            columns = columns,
            rows = rows
        )
    }

    private fun buildColumns(tagEntity: RuuviTagEntity, dataFormat: Int, isAir: Boolean): List<ExportColumn> {
        val columns = mutableListOf<ExportColumn>()

        // Date
        columns.add(ExportColumn(context.getString(R.string.date), 20.0))

        // Air Quality values
        if (isAir) {
            columns.add(ExportColumn(context.getString(R.string.aqi)))
            columns.add(ExportColumn("${context.getString(R.string.co2)} (${context.getString(R.string.unit_co2)})"))
            columns.add(ExportColumn("${context.getString(R.string.pm10)} (${context.getString(R.string.unit_pm10)})"))
            columns.add(ExportColumn("${context.getString(R.string.pm25)} (${context.getString(R.string.unit_pm25)})"))
            columns.add(ExportColumn("${context.getString(R.string.pm40)} (${context.getString(R.string.unit_pm40)})"))
            columns.add(ExportColumn("${context.getString(R.string.pm100)} (${context.getString(R.string.unit_pm100)})"))
            columns.add(ExportColumn("${context.getString(R.string.voc_index)}"))
            columns.add(ExportColumn("${context.getString(R.string.nox_index)}"))
        }

        // Temperature
        columns.add(ExportColumn("${context.getString(R.string.temperature)} (${context.getString(R.string.temperature_celsius_unit)})"))
        columns.add(ExportColumn("${context.getString(R.string.temperature)} (${context.getString(R.string.temperature_fahrenheit_unit)})"))
        columns.add(ExportColumn("${context.getString(R.string.temperature)} (${context.getString(R.string.temperature_kelvin_unit)})"))

        if (tagEntity.humidity != null) {
            // Humidity
            columns.add(ExportColumn("${context.getString(R.string.rel_humidity)} (${context.getString(R.string.humidity_relative_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.abs_humidity)} (${context.getString(R.string.humidity_absolute_unit)})"))

            // Dew point
            columns.add(ExportColumn("${context.getString(R.string.dewpoint)} (${context.getString(R.string.temperature_celsius_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.dewpoint)} (${context.getString(R.string.temperature_fahrenheit_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.dewpoint)} (${context.getString(R.string.temperature_kelvin_unit)})"))
        }

        if (tagEntity.pressure != null) {
            // Pressure in all units
            columns.add(ExportColumn("${context.getString(R.string.pressure)} (${context.getString(R.string.pressure_hpa_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.pressure)} (${context.getString(R.string.pressure_pa_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.pressure)} (${context.getString(R.string.pressure_mmhg_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.pressure)} (${context.getString(R.string.pressure_inhg_unit)})"))
        }

        // Movements (format 5)
        if (dataFormat == 5) {
            columns.add(ExportColumn("${context.getString(R.string.movements)}"))
        }

        // Battery voltage (format 3 / 5)
        if (dataFormat == 3 || dataFormat == 5) {
            columns.add(ExportColumn("${context.getString(R.string.battery)} (${context.getString(R.string.voltage_unit)})"))
        }

        // Acceleration (format 3 / 5)
        if (dataFormat == 3 || dataFormat == 5) {
            columns.add(ExportColumn("${context.getString(R.string.acceleration_x)} (${context.getString(R.string.acceleration_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.acceleration_y)} (${context.getString(R.string.acceleration_unit)})"))
            columns.add(ExportColumn("${context.getString(R.string.acceleration_z)} (${context.getString(R.string.acceleration_unit)})"))
        }

        // Signal Strength (RSSI)
        columns.add(ExportColumn("${context.getString(R.string.signal_strength)} (${context.getString(R.string.signal_unit)})"))

        // Measurement sequence number
        if (dataFormat == 5 || isAir) {
            columns.add(ExportColumn(context.getString(R.string.meas_seq_number)))
        }

        return columns
    }

    private fun buildRow(tagEntity: RuuviTagEntity, reading: TagSensorReading, dataFormat: Int, isAir: Boolean): List<Any?> {
        val row = mutableListOf<Any?>()

        // Date
        row.add(dateFormat.format(reading.createdAt))

        // Air Quality values
        if (isAir) {
            row.add(AQI.getAQI(reading.pm25, reading.co2).score?.roundHalfUp(1))
            row.add(reading.co2)
            row.add(reading.pm1)
            row.add(reading.pm25)
            row.add(reading.pm4)
            row.add(reading.pm10)
            row.add(reading.voc)
            row.add(reading.nox)
        }

        // Temperature
        val tempC = reading.temperature
        row.add(tempC?.roundHalfUp(2))
        row.add(tempC?.let { celsiusToFahrenheit(it).roundHalfUp(2) })
        row.add(tempC?.let { celsiusToKelvin(it).roundHalfUp(2) })

        if (tagEntity.humidity != null) {
            // Humidity
            val relHumidity = reading.humidity
            row.add(relHumidity?.roundHalfUp(2))
            row.add(
                if (tempC != null && relHumidity != null) {
                    unitsConverter.getHumidityValue(relHumidity, tempC, HumidityUnit.Absolute)
                } else null
            )

            // Dew point
            if (tempC != null && relHumidity != null) {
                val humidityConverter = HumidityConverter(tempC, relHumidity/100)
                row.add(humidityConverter.toDewCelsius?.roundHalfUp(2))
                row.add(humidityConverter.toDewFahrenheit?.roundHalfUp(2))
                row.add(humidityConverter.toDewKelvin?.roundHalfUp(2))
            }
        }

        // Pressure
        if (tagEntity.pressure != null) {
            val pressurePa = reading.pressure
            if (pressurePa != null) {
                row.add(
                    unitsConverter.getPressureValue(
                        pressurePa,
                        UnitType.PressureUnit.HectoPascal
                    )
                ) //hPa
                row.add(pressurePa) // Pa
                row.add(
                    unitsConverter.getPressureValue(
                        pressurePa,
                        UnitType.PressureUnit.MmHg
                    )
                ) // mmHg
                row.add(
                    unitsConverter.getPressureValue(
                        pressurePa,
                        UnitType.PressureUnit.InchHg
                    )
                ) // inHg
            }
        }

        // Movements (format 5)
        if (dataFormat == 5) {
            row.add(reading.movementCounter)
        }

        // Battery voltage
        if (dataFormat == 3 || dataFormat == 5) {
            row.add(reading.voltage?.roundHalfUp(3))
        }

        // Acceleration
        if (dataFormat == 3 || dataFormat == 5) {
            row.add(reading.accelX?.roundHalfUp(3))
            row.add(reading.accelY?.roundHalfUp(3))
            row.add(reading.accelZ?.roundHalfUp(3))
        }

        // Signal Strength (RSSI)
        row.add(reading.rssi)

        // Measurement sequence number
        if (dataFormat == 5 || isAir) {
            row.add(reading.measurementSequenceNumber)
        }

        return row
    }
}