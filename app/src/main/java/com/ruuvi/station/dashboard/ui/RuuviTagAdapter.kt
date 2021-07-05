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
import com.ruuvi.station.tag.domain.RuuviTag
import com.ruuvi.station.units.domain.UnitsConverter
import com.ruuvi.station.util.Utils
import com.ruuvi.station.util.extensions.describingTimeSince
import kotlinx.android.synthetic.main.item_dashboard.view.bell
import kotlinx.android.synthetic.main.item_dashboard.view.dashboardContainer
import kotlinx.android.synthetic.main.item_dashboard.view.deviceId
import kotlinx.android.synthetic.main.item_dashboard.view.humidity
import kotlinx.android.synthetic.main.item_dashboard.view.lastSeenTextView
import kotlinx.android.synthetic.main.item_dashboard.view.letterImage
import kotlinx.android.synthetic.main.item_dashboard.view.pressure
import kotlinx.android.synthetic.main.item_dashboard.view.movement
import kotlinx.android.synthetic.main.item_dashboard.view.temperature

class RuuviTagAdapter(
    private val activity: AppCompatActivity,
    items: List<RuuviTag>,
    private val converter: UnitsConverter
) : ArrayAdapter<RuuviTag>(activity, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view =
            convertView ?: LayoutInflater.from(context).inflate(R.layout.item_dashboard, parent, false)

        view.dashboardContainer.tag = item

        item?.let {
            view.deviceId.text = it.displayName
            view.temperature.text = it.temperatureString
            view.humidity.text = it.humidityString
            view.pressure.text = it.pressureString
            view.movement.text = it.movementCounter.toString()
        }

        val ballColorRes = if (position % 2 == 0) R.color.main else R.color.mainLight
        val ballRadius = context.resources.getDimension(R.dimen.letter_ball_radius).toInt()
        val ballColor = ContextCompat.getColor(context, ballColorRes)

        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val letterSize = 33 * displayMetrics.scaledDensity

        val ballBitmap = Utils.createBall(ballRadius, ballColor, Color.WHITE, view.deviceId.text.toString(), letterSize)
        view.letterImage.setImageBitmap(ballBitmap)

        val updatedAt = item?.updatedAt?.describingTimeSince(activity)
        view.lastSeenTextView.text = updatedAt

        val status = item?.status ?: AlarmStatus.NO_ALARM
        val (isVisible, iconResource) =
            when (status) {
                AlarmStatus.NO_ALARM -> true to R.drawable.ic_notifications_off_24px
                AlarmStatus.NO_TRIGGERED -> true to R.drawable.ic_notifications_on_24px
                AlarmStatus.TRIGGERED -> !view.bell.isVisible to R.drawable.ic_notifications_active_24px
            }
        view.bell.isVisible = isVisible
        view.bell.setImageResource(iconResource)

        val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.main))
        ImageViewCompat.setImageTintList(view.bell, colorStateList)

        return view
    }
}