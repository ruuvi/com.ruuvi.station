package com.ruuvi.station.widgets.domain

import android.os.SystemClock
import com.ruuvi.station.bluetooth.BluetoothLibrary
import com.ruuvi.station.bluetooth.contract.FoundRuuviTag
import com.ruuvi.station.database.domain.TagRepository
import com.ruuvi.station.network.data.response.SensorDenseResponse
import com.ruuvi.station.network.data.response.SensorsDenseInfo
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.widgets.data.WidgetSensorSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

internal class WidgetSensorSnapshotProvider(
    private val tagRepository: TagRepository,
    private val cloudInteractor: RuuviNetworkInteractor,
    private val decoder: (sensorId: String, encodedData: String, rssi: Int) -> FoundRuuviTag =
        { sensorId, encodedData, rssi ->
            BluetoothLibrary.decode(sensorId, encodedData, rssi)
        },
    private val monotonicTimeMillis: () -> Long = SystemClock::elapsedRealtime,
) {
    private var cachedCloudResponse: SensorDenseResponse? = null
    private var lastCloudRequestAtMillis: Long? = null
    private val cloudRequestMutex = Mutex()

    fun getFavoriteSensors(): List<RuuviTag> = tagRepository.getFavoriteSensors()

    fun getFavoriteSensor(sensorId: String): RuuviTag? =
        tagRepository.getFavoriteSensorById(sensorId)

    suspend fun getLatestSnapshot(sensorId: String): WidgetSensorSnapshot? =
        getFavoriteSensor(sensorId)?.let { getLatestSnapshot(it) }

    suspend fun getLatestSnapshot(sensor: RuuviTag): WidgetSensorSnapshot? {
        val localSnapshot = mapLocalSnapshot(sensor)
        if (!isCloudSensor(sensor)) return localSnapshot

        val cloudSnapshot = try {
            loadCloudSnapshot(sensor)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.e(error, "Unable to load cloud data for widget sensor ${sensor.id}")
            null
        }

        return if (
            cloudSnapshot != null &&
            (localSnapshot == null ||
                cloudSnapshot.timestampEpochMillis > localSnapshot.timestampEpochMillis)
        ) {
            cloudSnapshot
        } else {
            localSnapshot
        }
    }

    internal fun mapLocalSnapshot(sensor: RuuviTag): WidgetSensorSnapshot? {
        val measurement = sensor.latestMeasurement ?: return null
        return WidgetSensorSnapshot(
            sensorId = sensor.id,
            displayName = sensor.displayName,
            timestampEpochMillis = measurement.updatedAt.time,
            temperatureCelsius = measurement.temperature?.original.finiteOrNull(),
            relativeHumidityPercent = measurement.humidity?.original.finiteOrNull(),
            pressurePascal = measurement.pressure?.original.finiteOrNull(),
            movementCount = measurement.movement?.original.toExactIntOrNull(),
            voltageVolt = measurement.voltage.original.finiteOrNull(),
            rssiDbm = measurement.rssi.original.toExactIntOrNull(),
            measurementSequenceNumber = measurement.measurementSequenceNumber,
            accelerationXG = measurement.accelerationX.finiteOrNull(),
            accelerationYG = measurement.accelerationY.finiteOrNull(),
            accelerationZG = measurement.accelerationZ.finiteOrNull(),
            soundAverageDba = measurement.dBaAvg?.original.finiteOrNull(),
            soundPeakDba = measurement.dBaPeak?.original.finiteOrNull(),
            luminosityLux = measurement.luminosity?.original.finiteOrNull(),
            co2Ppm = measurement.co2?.original.toExactIntOrNull(),
            vocIndex = measurement.voc?.original.toExactIntOrNull(),
            noxIndex = measurement.nox?.original.toExactIntOrNull(),
            pm1MicrogramsPerCubicMeter = measurement.pm10?.original.finiteOrNull(),
            pm2_5MicrogramsPerCubicMeter = measurement.pm25?.original.finiteOrNull(),
            pm4MicrogramsPerCubicMeter = measurement.pm40?.original.finiteOrNull(),
            pm10MicrogramsPerCubicMeter = measurement.pm100?.original.finiteOrNull(),
        )
    }

    internal fun mapCloudSnapshot(
        sensor: RuuviTag,
        decoded: FoundRuuviTag,
        sensorInfo: SensorsDenseInfo,
        timestampEpochSeconds: Long,
    ): WidgetSensorSnapshot =
        WidgetSensorSnapshot(
            sensorId = sensor.id,
            displayName = sensor.displayName,
            timestampEpochMillis = Math.multiplyExact(timestampEpochSeconds, MILLIS_PER_SECOND),
            temperatureCelsius = decoded.temperature
                .plusOffset(sensorInfo.offsetTemperature),
            relativeHumidityPercent = decoded.humidity
                .plusOffset(sensorInfo.offsetHumidity),
            pressurePascal = decoded.pressure
                .plusOffset(sensorInfo.offsetPressure),
            movementCount = decoded.movementCounter,
            voltageVolt = decoded.voltage.finiteOrNull(),
            rssiDbm = decoded.rssi,
            measurementSequenceNumber = decoded.measurementSequenceNumber,
            accelerationXG = decoded.accelX.finiteOrNull(),
            accelerationYG = decoded.accelY.finiteOrNull(),
            accelerationZG = decoded.accelZ.finiteOrNull(),
            soundAverageDba = decoded.dBaAvg.finiteOrNull(),
            soundPeakDba = decoded.dBaPeak.finiteOrNull(),
            luminosityLux = decoded.luminosity.finiteOrNull(),
            co2Ppm = decoded.co2,
            vocIndex = decoded.voc,
            noxIndex = decoded.nox,
            pm1MicrogramsPerCubicMeter = decoded.pm1.finiteOrNull(),
            pm2_5MicrogramsPerCubicMeter = decoded.pm25.finiteOrNull(),
            pm4MicrogramsPerCubicMeter = decoded.pm4.finiteOrNull(),
            pm10MicrogramsPerCubicMeter = decoded.pm10.finiteOrNull(),
        )

    private suspend fun loadCloudSnapshot(sensor: RuuviTag): WidgetSensorSnapshot? {
        val cloudResponse = getCloudResponse()
            ?.takeIf { it.isSuccess() }
            ?: return null
        val sensorInfo = cloudResponse.data
            ?.sensors
            ?.firstOrNull { it.sensor == sensor.id }
            ?: return null
        val measurement = sensorInfo.measurements.maxByOrNull { it.timestamp } ?: return null
        val decoded = decoder(sensor.id, measurement.data, measurement.rssi)
        return mapCloudSnapshot(
            sensor = sensor,
            decoded = decoded,
            sensorInfo = sensorInfo,
            timestampEpochSeconds = measurement.timestamp,
        )
    }

    private suspend fun getCloudResponse(): SensorDenseResponse? = cloudRequestMutex.withLock {
        val now = monotonicTimeMillis()
        val lastRequestAt = lastCloudRequestAtMillis
        val refreshNeeded = lastRequestAt == null ||
            now - lastRequestAt >= CLOUD_DATA_REFRESH_INTERVAL_MILLIS

        if (refreshNeeded) {
            try {
                val candidate = cloudInteractor.getSensorDenseLastData()
                currentCoroutineContext().ensureActive()
                cachedCloudResponse = candidate
                lastCloudRequestAtMillis = monotonicTimeMillis()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                lastCloudRequestAtMillis = monotonicTimeMillis()
                return cachedCloudResponse ?: throw error
            }
        }

        cachedCloudResponse
    }

    private fun isCloudSensor(sensor: RuuviTag): Boolean = sensor.networkLastSync != null

    private fun Double?.plusOffset(offset: Double): Double? =
        if (this == null || !offset.isFinite()) {
            null
        } else {
            (this + offset).finiteOrNull()
        }

    private fun Double?.finiteOrNull(): Double? = this?.takeIf(Double::isFinite)

    private fun Double?.toExactIntOrNull(): Int? = this
        ?.takeIf {
            it.isFinite() &&
                it >= Int.MIN_VALUE.toDouble() &&
                it <= Int.MAX_VALUE.toDouble() &&
                it % 1.0 == 0.0
        }
        ?.toInt()

    companion object {
        private const val CLOUD_DATA_REFRESH_INTERVAL_MILLIS = 60_000L
        private const val MILLIS_PER_SECOND = 1_000L
    }
}
