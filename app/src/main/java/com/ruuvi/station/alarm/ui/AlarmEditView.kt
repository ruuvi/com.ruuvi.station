package com.ruuvi.station.alarm.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmCheckInteractor
import com.ruuvi.station.alarm.domain.AlarmElement
import com.ruuvi.station.alarm.domain.AlarmType
import com.ruuvi.station.database.domain.AlarmRepository
import com.ruuvi.station.databinding.ViewAlarmEditBinding
import com.ruuvi.station.network.domain.RuuviNetworkInteractor
import com.ruuvi.station.units.domain.UnitsConverter
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import timber.log.Timber
import java.text.DateFormat
import java.util.*
import kotlin.concurrent.schedule
import kotlin.math.round

class AlarmEditView @JvmOverloads
    constructor(
        private val ctx: Context,
        private val attributeSet: AttributeSet? = null,
        private val defStyleAttr: Int = 0)
    : ConstraintLayout(ctx, attributeSet, defStyleAttr) , KodeinAware {

    override val kodein: Kodein by kodein()
    private val unitsConverter: UnitsConverter by instance()
    private val networkInteractor: RuuviNetworkInteractor by instance()
    private val alarmRepository: AlarmRepository by instance()
    private val alarmCheckInteractor: AlarmCheckInteractor by instance()
    private lateinit var alarm: AlarmElement
    private var timer = Timer("AlarmEditView", true)

    var binding: ViewAlarmEditBinding
    private var useSeekBar = true

    init {
        val inflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ViewAlarmEditBinding.inflate(inflater, this)
    }

    fun restoreState(alarm: AlarmElement) {
        this.alarm = alarm
        if (alarm.type == AlarmType.MOVEMENT) useSeekBar = false

        with(binding.alertSwitch) {
            text = when (alarm.type) {
                AlarmType.TEMPERATURE -> ctx.getString(R.string.temperature_with_unit, unitsConverter.getTemperatureUnitString())
                AlarmType.HUMIDITY -> ctx.getString(R.string.humidity_with_unit, ctx.getString(R.string.humidity_relative_unit))
                AlarmType.PRESSURE -> ctx.getString(R.string.pressure_with_unit, unitsConverter.getPressureUnitString())
                AlarmType.RSSI -> ctx.getString(R.string.rssi)
                AlarmType.MOVEMENT -> ctx.getString(R.string.alert_movement)
            }
            isChecked = alarm.isEnabled
            setOnCheckedChangeListener { _, isChecked ->
                alarm.isEnabled = isChecked
                if (!isChecked) alarm.mutedTill = null
                updateUI()
                scheduleSaving()
            }
        }

        with(binding.alertSeekBar) {
            if (useSeekBar) {
                setMinValue(alarm.min.toFloat())
                setMaxValue(alarm.max.toFloat())
                setMinStartValue(alarm.low.toFloat())
                setMaxStartValue(alarm.high.toFloat())
                setGap(alarm.gap.toFloat())
                apply()

                setOnRangeSeekbarChangeListener { minValue, maxValue ->
                    alarm.low = minValue.toInt()
                    alarm.high = maxValue.toInt()
                    updateUI()
                    scheduleSaving()
                }
            } else {
                isVisible = false
                binding.alertMinValueTextView.isVisible = false
                binding.alertMaxValueTextView.isVisible = false
            }
        }

        with(binding.customDescriptionEditText) {
            setText(alarm.customDescription)
            addTextChangedListener {
                alarm.customDescription = it.toString()
                scheduleSaving()
            }
        }

        updateUI()
    }

    private fun updateUI() {
        var lowDisplay = alarm.low
        var highDisplay = alarm.high

        when (alarm.type) {
            AlarmType.TEMPERATURE -> {
                lowDisplay = round(unitsConverter.getTemperatureValue(alarm.low.toDouble())).toInt()
                highDisplay = round(unitsConverter.getTemperatureValue(alarm.high.toDouble())).toInt()
            }
            AlarmType.PRESSURE -> {
                lowDisplay = round(unitsConverter.getPressureValue(alarm.low.toDouble())).toInt()
                highDisplay = round(unitsConverter.getPressureValue(alarm.high.toDouble())).toInt()
            }
        }

        binding.alertSubtitleTextView.text = if (alarm.isEnabled) {
            when (alarm.type) {
                AlarmType.MOVEMENT -> ctx.getString(R.string.alert_movement_description)
                else -> String.format(ctx.getString(R.string.alert_subtitle_on), lowDisplay, highDisplay)
            }
        } else {
            ctx.getString(R.string.alert_subtitle_off)
        }

        binding.customDescriptionEditText.isGone = !alarm.isEnabled

        if (useSeekBar) {
            binding.alertSeekBar.isGone = !alarm.isEnabled
            binding.alertMaxValueTextView.isGone = !alarm.isEnabled
            binding.alertMinValueTextView.isGone = !alarm.isEnabled

            binding.alertMinValueTextView.text = lowDisplay.toString()
            binding.alertMaxValueTextView.text = highDisplay.toString()
        }

        with(binding.mutedTextView) {
            if (alarm.mutedTill ?: Date(0) > Date()) {
                text = DateFormat.getTimeInstance(DateFormat.SHORT).format(alarm.mutedTill)
                isGone = false
            } else {
                isGone = true
            }
        }
    }

    private fun scheduleSaving() {
        Timber.d("setTimer")
        timer.cancel()
        timer = Timer("AlarmEditView", true)
        timer.schedule(1500) {
            saveAlarm()
        }
    }

    fun saveAlarm() {
        Timber.d("saveAlarm-start")
        timer.cancel()
        if (alarm.shouldBeSaved()) {
            Timber.d("saveAlarm-shouldBeSaved")
            alarmRepository.saveAlarmElement(alarm)
            networkInteractor.setAlert(alarm.sensorId, alarm)
        }
        if (!alarm.isEnabled) {
            val notificationId = alarm.alarm?.id ?: -1
            removeNotificationById(notificationId)
        }
    }

    private fun removeNotificationById(notificationId: Int) {
        alarmCheckInteractor.removeNotificationById(notificationId)
    }
}
