package com.ruuvi.station.dashboard.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import com.ruuvi.station.R
import com.ruuvi.station.alarm.domain.AlarmStatus
import com.ruuvi.station.databinding.ItemDashboardBinding
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.describingTimeSince

class RuuviTagAdapter(
    private val activity: AppCompatActivity,
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
            binding.temperature.text = it.temperatureString
            binding.humidity.text = it.humidityString
            binding.pressure.text = it.pressureString
            binding.movement.text =   it.movementCounterString
        }

        val ballColorRes = if (position % 2 == 0) R.color.main else R.color.mainLight
        val ballRadius = context.resources.getDimension(R.dimen.letter_ball_radius).toInt()
        val ballColor = ContextCompat.getColor(context, ballColorRes)

        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val letterSize = 33 * displayMetrics.scaledDensity

        val ballBitmap = Utils.createBall(ballRadius, ballColor, Color.WHITE, binding.deviceId.text.toString(), letterSize)
        binding.letterImage.setImageBitmap(ballBitmap)

        val updatedAt = item?.updatedAt?.describingTimeSince(activity)
        binding.lastSeenTextView.text = updatedAt

        val status = item?.status ?: AlarmStatus.NO_ALARM
        val (isVisible, iconResource) =
            when (status) {
                AlarmStatus.NO_ALARM -> true to R.drawable.ic_notifications_off_24px
                AlarmStatus.NO_TRIGGERED -> true to R.drawable.ic_notifications_on_24px
                AlarmStatus.TRIGGERED -> !binding.bell.isVisible to R.drawable.ic_notifications_active_24px
            }
        binding.bell.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
        binding.bell.setImageResource(iconResource)

        val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
        ImageViewCompat.setImageTintList(binding.bell, colorStateList)

        return binding.root
    }
}