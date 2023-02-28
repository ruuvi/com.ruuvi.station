package com.ruuvi.station.dashboard.ui

import android.graphics.Color
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmSensorStatus
import com.ruuvi.station.databinding.ItemDashboardBinding
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.MovementConverter
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.describingTimeSince

class RuuviTagAdapter(
    private val activity: AppCompatActivity,
    private val unitsConverter: UnitsConverter,
    private val movementConverter: MovementConverter,
    items: List<RuuviTag>
) : ArrayAdapter<RuuviTag>(activity, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val binding = if (convertView != null) {
            ItemDashboardBinding.bind(convertView)
        } else {
            ItemDashboardBinding.inflate(LayoutInflater.from(context), parent, false)
        }
        binding.dashboardContainer.tag = item

        item?.let {
            binding.deviceId.text = it.displayName
            binding.temperature.text = unitsConverter.getTemperatureStringWithoutUnit(it.temperature)
            binding.temperatureUnit.text = activity.getText(unitsConverter.getTemperatureUnit().unit)
            binding.humidity.text = unitsConverter.getHumidityStringWithoutUnit(it.humidity, it.temperature)
            binding.humidityUnit.text = if (it.humidity != null) unitsConverter.getHumidityUnitString() else ""
            binding.pressure.text = unitsConverter.getPressureStringWithoutUnit(it.pressure)
            binding.pressureUnit.text = if (it.pressure != null) activity.getText(unitsConverter.getPressureUnit().unit) else ""
            binding.movement.text =   movementConverter.getMovementStringWithoutUnit(it.movementCounter)
            binding.movementUnit.text = if (it.movementCounter != null) activity.getText(R.string.movements) else ""
        }

        val ballColorRes = if (position % 2 == 0) R.color.keppel else R.color.elm
        val ballRadius = context.resources.getDimension(R.dimen.letter_ball_radius).toInt()
        val ballColor = ContextCompat.getColor(context, ballColorRes)

        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val letterSize = 28 * displayMetrics.scaledDensity

        val ballBitmap = Utils.createBall(activity, ballRadius, ballColor, Color.WHITE, binding.deviceId.text.toString(), letterSize)
        binding.letterImage.setImageBitmap(ballBitmap)

        val updatedAt = item?.updatedAt?.describingTimeSince(activity)
        binding.lastSeenTextView.text = updatedAt

        val status = item?.status ?: AlarmSensorStatus.NoAlarms
        val (isVisible, iconResource, alpha) =
            when (status) {
                is AlarmSensorStatus.NoAlarms -> Triple(true, R.drawable.ic_notifications_off_24px, 0.5f)
                is AlarmSensorStatus.NotTriggered -> Triple(true, R.drawable.ic_notifications_on_24px, 1f)
                is AlarmSensorStatus.Triggered -> Triple(!binding.bell.isVisible, R.drawable.ic_notifications_active_24px, 1f)
            }
        binding.bell.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        binding.bell.setImageResource(iconResource)
        binding.bell.alpha = alpha

        return binding.root
    }
}